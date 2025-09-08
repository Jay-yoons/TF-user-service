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
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

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
    private final CognitoIdentityProviderClient cognitoClient;
    
    public AwsCognitoService(AwsCognitoConfig cognitoConfig, JwtTokenUtil jwtTokenUtil) {
        this.cognitoConfig = cognitoConfig;
        this.jwtTokenUtil = jwtTokenUtil;
        this.restTemplate = new RestTemplate();
        this.cognitoClient = CognitoIdentityProviderClient.builder().build();
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
            logger.info("=== 토큰 교환 시작 ===");
            logger.info("인증 코드: {}", authorizationCode);
            logger.info("토큰 엔드포인트: {}", cognitoConfig.getTokenEndpoint());
            logger.info("클라이언트 ID: {}", cognitoConfig.getClientId());
            logger.info("리다이렉트 URI: {}", cognitoConfig.getRedirectUri());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", cognitoConfig.getGrantType());
            body.add("client_id", cognitoConfig.getClientId());
            body.add("client_secret", cognitoConfig.getClientSecret());
            body.add("code", authorizationCode);
            body.add("redirect_uri", cognitoConfig.getRedirectUri());
            
            logger.info("요청 바디 생성 완료");
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            logger.info("Cognito 토큰 엔드포인트로 요청 전송...");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                cognitoConfig.getTokenEndpoint(), 
                request, 
                Map.class
            );
            
            logger.info("Cognito 응답 수신: status={}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                logger.info("토큰 교환 성공: access_token 존재={}, id_token 존재={}", 
                    tokenResponse.containsKey("access_token"), 
                    tokenResponse.containsKey("id_token"));
                logger.info("=== 토큰 교환 성공 완료 ===");
                return tokenResponse;
            } else {
                logger.error("토큰 교환 실패: status={}, body={}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("토큰 교환에 실패했습니다.");
            }
            
        } catch (Exception e) {
            logger.error("=== 토큰 교환 중 오류 발생 ===");
            logger.error("예외 타입: {}", e.getClass().getSimpleName());
            logger.error("예외 메시지: {}", e.getMessage());
            logger.error("예외 상세 정보:", e);
            throw new RuntimeException("토큰 교환 중 오류가 발생했습니다.", e);
        }
    }

    
    /**
     * 사용자 정보 조회 (ID 토큰에서)
     */
    public Map<String, Object> getUserInfoFromIdToken(String idToken) {
        try {
            logger.info("=== ID 토큰에서 사용자 정보 조회 시작 ===");
            logger.info("ID 토큰: {}", idToken != null ? "존재함" : "null");
            
            if (idToken == null) {
                logger.error("ID 토큰이 null입니다");
                throw new RuntimeException("ID 토큰이 null입니다");
            }
            
            // JWT 토큰에서 사용자 정보 추출
            logger.info("JWT 토큰에서 사용자 정보 추출 시작...");
            Map<String, Object> userInfo = jwtTokenUtil.getUserInfoFromToken(idToken);
            
            logger.info("사용자 정보 추출 성공: sub={}, keys={}", userInfo.get("sub"), userInfo.keySet());
            logger.info("=== ID 토큰에서 사용자 정보 조회 완료 ===");
            return userInfo;
            
        } catch (Exception e) {
            logger.error("=== 사용자 정보 조회 중 오류 발생 ===");
            logger.error("예외 타입: {}", e.getClass().getSimpleName());
            logger.error("예외 메시지: {}", e.getMessage());
            logger.error("예외 상세 정보:", e);
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
    
    /**
     * Cognito 사용자 삭제 (Admin API 사용)
     */
    public boolean deleteCognitoUser(String userId) {
        try {
            logger.info("=== Cognito 사용자 삭제 시작 ===");
            logger.info("삭제할 사용자 ID: {}", userId);
            logger.info("사용자 풀 ID: {}", cognitoConfig.getUserPoolId());
            
            AdminDeleteUserRequest deleteRequest = AdminDeleteUserRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(userId)
                    .build();
            
            AdminDeleteUserResponse deleteResponse = cognitoClient.adminDeleteUser(deleteRequest);
            
            logger.info("Cognito 사용자 삭제 성공: userId={}", userId);
            logger.info("=== Cognito 사용자 삭제 완료 ===");
            return true;
            
        } catch (CognitoIdentityProviderException e) {
            logger.error("Cognito 사용자 삭제 실패: userId={}, error={}", userId, e.awsErrorDetails().errorMessage());
            return false;
        } catch (Exception e) {
            logger.error("Cognito 사용자 삭제 중 예외 발생: userId={}", userId, e);
            return false;
        }
    }
}
