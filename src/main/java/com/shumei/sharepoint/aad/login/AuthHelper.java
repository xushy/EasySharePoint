package com.shumei.sharepoint.aad.login;


import com.microsoft.aad.msal4j.AuthorizationCodeParameters;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.shumei.sharepoint.configuration.AadConfig;
import com.shumei.sharepoint.util.UserSharePoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Decoder;

import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateKeySpec;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Helpers for acquiring authorization codes and tokens from AAD
 */
@Component
public class AuthHelper {
    public static final String PRINCIPAL_SESSION_NAME = "principal";
    public static final String TOKEN_CACHE_SESSION_ATTRIBUTE = "token_cache";

    @Autowired
    AadConfig aadConfig;
    @Autowired
    SpringContext springContext;

    public AadLoginResult processAuthenticationCodeRedirect(HttpServletRequest httpRequest, String currentUri, String fullUrl) throws Throwable {
        Map<String, List<String>> params = new HashMap<>();
        for (String key : httpRequest.getParameterMap().keySet()) {
            params.put(key, Collections.singletonList(httpRequest.getParameterMap().get(key)[0]));
        }
        StateData stateData = SessionManagementHelper.validateState(params.get(SessionManagementHelper.STATE).get(0));
        AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(fullUrl), params);
        AadLoginResult loginResult = new AadLoginResult();
        if (AuthHelper.isAuthenticationSuccessful(authResponse)) {
            AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
            validateAuthRespMatchesAuthCodeFlow(oidcResponse);
            String email = null;
            if (aadConfig.getEnableSharePoint()) {
                AuthorizationCodeProviderExt authProvider = new AuthorizationCodeProviderExt(aadConfig.getClientId(), Arrays.asList(UserSharePoint.SCOPE), oidcResponse.getAuthorizationCode().getValue(), currentUri, NationalCloud.Global, aadConfig.getTenantId(), aadConfig.getClientSecret());
                IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
                OAuthJSONAccessTokenResponse response = authProvider.getResponse();
                String idToken = response.getParam("id_token");
                email = (String) JWTParser.parse(idToken).getJWTClaimsSet().getClaim("preferred_username");
                loginResult.setGraphServiceClient(graphClient);
                UserSharePoint userSharePoint = springContext.getApplicationContext().getBean(UserSharePoint.class, graphClient, email);
                System.out.println(userSharePoint.toString());
                loginResult.setUserSharePoint(userSharePoint);
            } else {
                IAuthenticationResult result = getAuthResultByAuthCode(httpRequest, oidcResponse.getAuthorizationCode(), currentUri);
                validateNonce(stateData, getNonceClaimValueFromIdToken(result.idToken()));
                email = result.account().username();
            }
            loginResult.setSuccess(true);
            loginResult.setEmail(email);
        } else {
            AuthenticationErrorResponse response = (AuthenticationErrorResponse) authResponse;
            loginResult.setSuccess(false);
            loginResult.setErrorCode(response.getErrorObject().getCode());
            loginResult.setErrorMsg(response.getErrorObject().getDescription());
        }
        return loginResult;
    }

    private void validateNonce(StateData stateData, String nonce) throws Exception {
        if (StringUtils.isEmpty(nonce) || !nonce.equals(stateData.getNonce())) {
            throw new Exception(SessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "could not validate nonce");
        }
    }

    private String getNonceClaimValueFromIdToken(String idToken) throws ParseException {
        return (String) JWTParser.parse(idToken).getJWTClaimsSet().getClaim("nonce");
    }

    private void validateAuthRespMatchesAuthCodeFlow(AuthenticationSuccessResponse oidcResponse) throws Exception {
        if (oidcResponse.getIDToken() != null || oidcResponse.getAccessToken() != null || oidcResponse.getAuthorizationCode() == null) {
            throw new Exception(SessionManagementHelper.FAILED_TO_VALIDATE_MESSAGE + "unexpected set of artifacts received");
        }
    }

    public void sendAuthRedirect(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String scope, String redirectURL)
            throws IOException {

        // state parameter to validate response from Authorization server and nonce parameter to validate idToken
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        SessionManagementHelper.storeStateAndNonce(state, nonce);

        httpResponse.setStatus(302);
        String authorizationCodeUrl = getAuthorizationCodeUrl(httpRequest.getParameter("claims"), scope, redirectURL, state, nonce);
        httpResponse.sendRedirect(authorizationCodeUrl);
    }

    public String getAuthorizationCodeUrl(String claims, String scope, String registeredRedirectURL, String state, String nonce)
            throws UnsupportedEncodingException {

        String urlEncodedScopes = scope == null ?
                URLEncoder.encode("openid offline_access profile", "UTF-8") :
                URLEncoder.encode("openid offline_access profile" + " " + scope, "UTF-8");


        String authorizationCodeUrl = aadConfig.getAuthority() + "oauth2/v2.0/authorize?" +
                "response_type=code&" +
                "response_mode=form_post&" +
                "redirect_uri=" + URLEncoder.encode(registeredRedirectURL, "UTF-8") +
                "&client_id=" + aadConfig.getClientId() +
                "&scope=" + urlEncodedScopes +
                (StringUtils.isEmpty(claims) ? "" : "&claims=" + claims) +
                "&prompt=select_account" +
                "&state=" + state
                + "&nonce=" + nonce;

        return authorizationCodeUrl;
    }

    private IAuthenticationResult getAuthResultByAuthCode(HttpServletRequest httpServletRequest, AuthorizationCode authorizationCode, String currentUri) throws Throwable {
        IAuthenticationResult result;
        ConfidentialClientApplication app;
        try {
            app = createClientApplication();

            String authCode = authorizationCode.getValue();
            AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                    authCode,
                    new URI(currentUri)).
                    build();

            Future<IAuthenticationResult> future = app.acquireToken(parameters);

            result = future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }

    private ConfidentialClientApplication createClientApplication() throws Exception {
        if (aadConfig.getSecret()) {
            return ConfidentialClientApplication.builder(aadConfig.getClientId(), ClientCredentialFactory.createFromSecret(aadConfig.getClientSecret())).
                    authority(aadConfig.getAuthority()).
                    build();
        } else {
            PrivateKey privateKey = this.getPrivateKey(aadConfig.getPrivateKey());
            X509Certificate x509Certificate = this.readCertificate(aadConfig.getPublicKey());
            return ConfidentialClientApplication.builder(aadConfig.getClientId(), ClientCredentialFactory.createFromCertificate(privateKey, x509Certificate)).
                    authority(aadConfig.getAuthority()).
                    build();
        }

    }

    private static boolean isAuthenticationSuccessful(AuthenticationResponse authResponse) {
        return authResponse instanceof AuthenticationSuccessResponse;
    }

    public PrivateKey readPrivateKey(String filename) throws Exception {
        PEMParser pemParser = new PEMParser(new FileReader(filename));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
        KeyPair kp = converter.getKeyPair(pemKeyPair);
        return kp.getPrivate();
    }

    public PrivateKey getPrivateKey(String filename) throws Exception {
        BASE64Decoder base64decoder = new BASE64Decoder();
        BufferedReader br8 = new BufferedReader(new FileReader(filename));
        String s = br8.readLine();
        String str = "";
        s = br8.readLine();
        while (s.charAt(0) != '-') {
            str += s + "\r";
            s = br8.readLine();
        }
        byte[] buffer8 = base64decoder.decodeBuffer(str);
        br8.close();
        RSAPrivateKeyStructure asn1PrivKey = new RSAPrivateKeyStructure((ASN1Sequence) ASN1Sequence.fromByteArray(buffer8));
        RSAPrivateKeySpec rsaPrivKeySpec = new RSAPrivateKeySpec(asn1PrivKey.getModulus(), asn1PrivKey.getPrivateExponent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey priKey = keyFactory.generatePrivate(rsaPrivKeySpec);
        return priKey;
    }

    public X509Certificate readCertificate(String filename) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(filename));
        return x509Certificate;
    }
}