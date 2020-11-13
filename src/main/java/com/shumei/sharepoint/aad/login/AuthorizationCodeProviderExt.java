package com.shumei.sharepoint.aad.login;

import com.microsoft.graph.auth.confidentialClient.AuthorizationCodeProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;

import java.util.List;

/**
 * @author xushuai
 * @description 因BaseAuthentication的getResponse方法由protected修饰，无法在包外的非继承类获得response，故写此类
 */
public class AuthorizationCodeProviderExt extends AuthorizationCodeProvider {
    public AuthorizationCodeProviderExt(String clientId, List<String> scopes, String authorizationCode, String redirectUri, String clientSecret) {
        super(clientId, scopes, authorizationCode, redirectUri, clientSecret);
    }

    public AuthorizationCodeProviderExt(String clientId, List<String> scopes, String authorizationCode, String redirectUri, NationalCloud nationalCloud, String tenant, String clientSecret) {
        super(clientId, scopes, authorizationCode, redirectUri, nationalCloud, tenant, clientSecret);
    }

    @Override
    public OAuthJSONAccessTokenResponse getResponse() {
        return super.getResponse();
    }
}
