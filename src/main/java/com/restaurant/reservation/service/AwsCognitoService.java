package com.restaurant.reservation.service;

import com.restaurant.reservation.config.AwsCognitoConfig;
import com.restaurant.reservation.config.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AWS Cognito 서비스 클래스
 * 
 * AWS Cognito와의 통신을 담당합니다.
 * - 사용자 인증
 * - 토큰 검증
 * - 사용자 정보 조회
 * 
 * @author Team-FOG
 * @version 1.0
 * @since 2024-01-15
 */
@Service
public class AwsCognitoService {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoService.class);
    
    private final AwsCognitoConfig cognitoConfig;
    private final RestTemplate restTemplate;
    private final JwtTokenUtil jwtTokenUtil;
    
    public AwsCognitoService(AwsCognitoConfig cognitoConfig, JwtTokenUtil jwtTokenUtil) {
        this.cognitoConfig = cognitoConfig;
        this.jwtTokenUtil = jwtTokenUtil;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Cognito 로그인 URL 생성
     */
    public String generateLoginUrl(String state) {
        String loginUrl = cognitoConfig.getAuthorizeEndpoint() +
                "?response_type=" + cognitoConfig.getResponseType() +
                "&client_id=" + cognitoConfig.getClientId() +
                "&redirect_uri=" + cognitoConfig.getRedirectUri() +
                "&scope=" + cognitoConfig.getScope() +
                "&state=" + state;
        
        logger.info("Cognito 로그인 URL 생성: {}", loginUrl);
        return loginUrl;
    }
    
    /**
     * 인증 코드로 액세스 토큰 교환
     */
    public Map<String, Object> exchangeCodeForToken(String authorizationCode) {
        try {
            logger.info("인증 코드로 토큰 교환 시작: code={}", authorizationCode);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", cognitoConfig.getGrantType());
            body.add("client_id", cognitoConfig.getClientId());
            body.add("client_secret", cognitoConfig.getClientSecret());
            body.add("code", authorizationCode);
            body.add("redirect_uri", cognitoConfig.getRedirectUri());
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                cognitoConfig.getTokenEndpoint(), 
                request, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                logger.info("토큰 교환 성공: access_token 존재={}", tokenResponse.containsKey("access_token"));
                return tokenResponse;
            } else {
                logger.error("토큰 교환 실패: status={}", response.getStatusCode());
                throw new RuntimeException("토큰 교환에 실패했습니다.");
            }
            
        } catch (Exception e) {
            logger.error("토큰 교환 중 오류 발생", e);
            throw new RuntimeException("토큰 교환 중 오류가 발생했습니다.", e);
        }
    }

    
    /**
     * 사용자 정보 조회 (ID 토큰에서)
     */
    public Map<String, Object> getUserInfoFromIdToken(String idToken) {
        try {
            logger.info("ID 토큰에서 사용자 정보 조회");
            
            // JWT 토큰에서 사용자 정보 추출
            Map<String, Object> userInfo = jwtTokenUtil.getUserInfoFromToken(idToken);
            
            logger.debug("사용자 정보 추출 성공: sub={}", userInfo.get("sub"));
            return userInfo;
            
        } catch (Exception e) {
            logger.error("사용자 정보 조회 중 오류 발생", e);
            throw new RuntimeException("사용자 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            logger.debug("토큰 유효성 검증 시작");
            
            // JWT 토큰 검증
            boolean isValid = jwtTokenUtil.validateToken(token);
            
            if (isValid) {
                logger.debug("토큰 검증 성공");
            } else {
                logger.warn("토큰 검증 실패");
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("토큰 검증 중 오류 발생", e);
            return false;
        }
    }
}
