package com.shumei.sharepoint.configuration;

import com.microsoft.graph.auth.enums.NationalCloud;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class AadConfig {
    @Value("${aad.login}")
    private Boolean needAadLogin;
    @Value("${aad.clientId}")
    private String clientId;
    @Value("${aad.tenantId}")
    private String tenantId;
    @Value("${aad.clientSecret}")
    private String clientSecret;
    @Value("${aad.nationalCloud:Global}")
    private NationalCloud nationalCloud;
    @Value("${aad.authority}")
    private String authority;
    @Value("${aad.redirectUriSignin}")
    private String redirectUriSignIn;
    @Value("${aad.isSecret:false}")
    private Boolean isSecret;
    @Value("${aad.privateKey}")
    private String privateKey;
    @Value("${aad.publicKey}")
    private String publicKey;
    @Value("${aad.sharepoint.enable}")
    private Boolean enableSharePoint;

    public Boolean getNeedAadLogin() {
        return needAadLogin;
    }

    public void setNeedAadLogin(Boolean needAadLogin) {
        this.needAadLogin = needAadLogin;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public NationalCloud getNationalCloud() {
        return nationalCloud;
    }

    public void setNationalCloud(NationalCloud nationalCloud) {
        this.nationalCloud = nationalCloud;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getRedirectUriSignIn() {
        return redirectUriSignIn;
    }

    public void setRedirectUriSignIn(String redirectUriSignIn) {
        this.redirectUriSignIn = redirectUriSignIn;
    }

    public Boolean getSecret() {
        return isSecret;
    }

    public void setSecret(Boolean secret) {
        isSecret = secret;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Boolean getEnableSharePoint() {
        return enableSharePoint;
    }

    public void setEnableSharePoint(Boolean enableSharePoint) {
        this.enableSharePoint = enableSharePoint;
    }
}
