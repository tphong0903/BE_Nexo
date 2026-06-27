package org.nexo.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakConfig {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String adminUsername;
    private String adminPassword;

    /**
     * URL FE để Keycloak điều hướng về sau khi user xác minh email xong.
     * Phải nằm trong "Valid Redirect URIs" của client {@link #verifyEmailClientId}.
     */
    private String verifyEmailRedirectUri;

    /**
     * Client dùng cho redirect sau khi verify email. Mặc định trùng clientId, nhưng
     * thường nên trỏ tới client của FE (client có đăng ký redirect URI ở trên).
     */
    private String verifyEmailClientId;

    public String getTokenUrl() {
        return serverUrl + "/realms/master/protocol/openid-connect/token";
    }

    public String getRefreshTokenUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String getLoginUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String getOAuthLoginUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String getLogoutUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    public String getUsersUrl() {
        return serverUrl + "/admin/realms/" + realm + "/users";
    }

}