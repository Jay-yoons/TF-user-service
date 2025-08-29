package com.restaurant.reservation.config;

import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
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
