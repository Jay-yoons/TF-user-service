package com.restaurant.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * AWS Cognito 설정 클래스
 * 
 * application.yml의 aws.cognito 설정을 바인딩합니다.
 * 
 * @author Team-FOG
 * @version 1.0
 * @since 2024-01-15
 */
@Component
@ConfigurationProperties(prefix = "aws.cognito")
public class AwsCognitoConfig {
    
    private String region;
    private String userPoolId;
    private String clientId;
    private String clientSecret;
    private String domain;
    private String jwksUrl;
    private String tokenEndpoint;
    private String authorizeEndpoint;
    private String logoutEndpoint;
    private String redirectUri;
    private String scope;
    private String responseType;
    private String grantType;
    
    // Getters and Setters
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getUserPoolId() {
        return userPoolId;
    }
    
    public void setUserPoolId(String userPoolId) {
        this.userPoolId = userPoolId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getJwksUrl() {
        return jwksUrl;
    }
    
    public void setJwksUrl(String jwksUrl) {
        this.jwksUrl = jwksUrl;
    }
    
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }
    
    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }
    
    public String getAuthorizeEndpoint() {
        return authorizeEndpoint;
    }
    
    public void setAuthorizeEndpoint(String authorizeEndpoint) {
        this.authorizeEndpoint = authorizeEndpoint;
    }
    
    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }
    
    public void setLogoutEndpoint(String logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    
    public String getGrantType() {
        return grantType;
    }
    
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
    
    @PostConstruct
    public void printConfig() {
        System.out.println("=== AWS Cognito Configuration ===");
        System.out.println("Region: " + region);
        System.out.println("User Pool ID: " + userPoolId);
        System.out.println("Client ID: " + clientId);
        System.out.println("Domain: " + domain);
        System.out.println("JWKS URL: " + jwksUrl);
        System.out.println("Redirect URI: " + redirectUri);
        System.out.println("================================");
    }
}
