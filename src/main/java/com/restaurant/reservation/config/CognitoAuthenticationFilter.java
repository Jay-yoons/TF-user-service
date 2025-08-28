package com.restaurant.reservation.config;

import com.restaurant.reservation.service.AwsCognitoService;
import com.restaurant.reservation.config.AwsCognitoConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * AWS Cognito 인증 필터
 * 
 * 요청의 Authorization 헤더에서 Cognito JWT 토큰을 추출하고 검증합니다.
 * 
 * @author Team-FOG
 * @version 1.0
 * @since 2024-01-15
 */
@Component
@Slf4j
public class CognitoAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(CognitoAuthenticationFilter.class);
    
    private final AwsCognitoService cognitoService;
    private final AwsCognitoConfig cognitoConfig;
    private final UserDetailsService userDetailsService;
    
    public CognitoAuthenticationFilter(AwsCognitoService cognitoService, AwsCognitoConfig cognitoConfig, UserDetailsService userDetailsService) {
        this.cognitoService = cognitoService;
        this.cognitoConfig = cognitoConfig;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String[] publicPaths = {
                "/api/users/login",
                "/api/users/login/url",
                "/api/users/login/callback",
                "/health",
                "/api/health/**",
                "/actuator/**"
        };
        String requestURI = request.getRequestURI();
        log.info("Filtering check, requestURI={}", requestURI);
        for (String path : publicPaths) {
            if (requestURI.startsWith(path.replace("/**", ""))) {
                log.info("shouldNotFilter TRUE for path={}", path);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        log.info("[Request Info] Method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        log.info("[Request Headers] Origin: {}, Authorization Exists: {}",
                request.getHeader("Origin"),
                request.getHeader("Authorization") != null);
        try {
            String token = extractTokenFromRequest(request);
            log.info("[doFilterInternal] Authorization 헤더에서 토큰 추출: {}", token != null ? "존재함" : "없음");
            
            if (token != null && cognitoService.validateToken(token)) {
                log.info("[doFilterInternal] 토큰 유효성 검사 성공");
                // 토큰이 유효한 경우 사용자 정보를 추출
                Map<String, Object> userInfo = cognitoService.getUserInfoFromIdToken(token);
                String userId = (String) userInfo.get("sub");
                
                if (userId != null) {
                    // 데이터베이스에서 사용자 정보 조회
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Cognito 인증 성공: userId={}", userId);
                }
            }
            
        } catch (Exception e) {
            logger.error("Cognito 인증 처리 중 오류 발생", e);
            // 인증 실패 시에도 요청을 계속 진행 (인증이 필요한 엔드포인트는 별도로 처리)
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 요청에서 JWT 토큰을 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        
        return null;
    }
    

}
