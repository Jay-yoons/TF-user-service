package com.restaurant.reservation.config;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * JWT 토큰 유틸리티 클래스
 * 
 * AWS Cognito JWT 토큰의 검증과 디코딩을 담당합니다.
 * 
 * @author Team-FOG
 * @version 1.0
 * @since 2024-01-15
 */
@Component
public class JwtTokenUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    private final AwsCognitoConfig cognitoConfig;
    private final ObjectMapper objectMapper;
    
    public JwtTokenUtil(AwsCognitoConfig cognitoConfig) {
        this.cognitoConfig = cognitoConfig;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * JWT 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                logger.warn("토큰이 null이거나 비어있습니다");
                return false;
            }
            
            // JWT 토큰 디코딩 (서명 검증 없이)
            DecodedJWT decodedJWT = JWT.decode(token);
            
            // 토큰 타입 확인 (ID 토큰 또는 Access 토큰 허용)
            String tokenUse = decodedJWT.getClaim("token_use").asString();
            if (!"id".equals(tokenUse) && !"access".equals(tokenUse)) {
                logger.warn("토큰 타입이 올바르지 않습니다: {}", tokenUse);
                return false;
            }
            
            // 발급자(issuer) 확인
            String issuer = decodedJWT.getIssuer();
            String expectedIssuer = "https://cognito-idp." + cognitoConfig.getRegion() + ".amazonaws.com/" + cognitoConfig.getUserPoolId();
            if (!expectedIssuer.equals(issuer)) {
                logger.warn("토큰 발급자가 올바르지 않습니다: {}", issuer);
                return false;
            }
            
            // 대상(audience) 확인 (Access Token의 경우 aud 클레임이 없을 수 있음)
            List<String> audiences = decodedJWT.getAudience();
            if (audiences != null && !audiences.isEmpty()) {
                String audience = audiences.get(0);
                if (!cognitoConfig.getClientId().equals(audience)) {
                    logger.warn("토큰 대상이 올바르지 않습니다: {}", audience);
                    return false;
                }
            } else {
                logger.debug("토큰에 audience 클레임이 없습니다 (Access Token일 가능성)");
            }
            
            // 만료 시간 확인
            if (decodedJWT.getExpiresAt() == null || decodedJWT.getExpiresAt().getTime() < System.currentTimeMillis()) {
                logger.warn("토큰이 만료되었습니다");
                return false;
            }
            
            logger.debug("JWT 토큰 검증 성공");
            return true;
            
        } catch (Exception e) {
            logger.error("JWT 토큰 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * JWT 토큰에서 사용자 정보 추출
     */
    public Map<String, Object> getUserInfoFromToken(String token) {
        try {
            // JWT 토큰 디코딩
            DecodedJWT decodedJWT = JWT.decode(token);
            
            // 페이로드에서 사용자 정보 추출
            String payload = decodedJWT.getPayload();
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = objectMapper.readValue(decodedPayload, Map.class);
            
            logger.debug("JWT 토큰에서 사용자 정보 추출 성공: sub={}", userInfo.get("sub"));
            return userInfo;
            
        } catch (Exception e) {
            logger.error("JWT 토큰에서 사용자 정보 추출 중 오류 발생", e);
            throw new RuntimeException("사용자 정보 추출 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * JWT 토큰에서 특정 클레임 추출
     */
    public String getClaimFromToken(String token, String claimName) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getClaim(claimName).asString();
        } catch (Exception e) {
            logger.error("JWT 토큰에서 클레임 추출 중 오류 발생: claim={}", claimName, e);
            return null;
        }
    }
    
    /**
     * RSA 공개키로 JWT 서명 검증 (선택적)
     * 실제 운영에서는 JWKS를 통해 공개키를 가져와서 검증
     */
    public boolean verifyTokenSignature(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            String kid = decodedJWT.getKeyId();
            
            // JWKS에서 공개키 가져오기
            JwkProvider provider = new UrlJwkProvider(new URL(cognitoConfig.getJwksUrl()));
            Jwk jwk = provider.get(kid);
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();
            
            // 알고리즘 생성
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            
            // JWT 검증기 생성
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("https://cognito-idp." + cognitoConfig.getRegion() + ".amazonaws.com/" + cognitoConfig.getUserPoolId())
                    .withAudience(cognitoConfig.getClientId())
                    .build();
            
            // 토큰 검증
            verifier.verify(token);
            
            logger.debug("JWT 서명 검증 성공");
            return true;
            
        } catch (JWTVerificationException e) {
            logger.warn("JWT 서명 검증 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("JWT 서명 검증 중 오류 발생", e);
            return false;
        }
    }
}
