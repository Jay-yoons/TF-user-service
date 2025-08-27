//package com.restaurant.reservation.config;
//
//import com.restaurant.reservation.service.AwsCognitoService;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Map;
//
///**
// * ALB Cognito 인증 필터
// *
// * ALB에서 Cognito 인증을 처리한 후 전달되는 헤더에서 사용자 정보를 추출합니다.
// *
// * @author Team-FOG
// * @version 1.0
// * @since 2024-01-15
// */
//@Component
//public class AlbCognitoAuthenticationFilter extends OncePerRequestFilter {
//
//    private static final Logger logger = LoggerFactory.getLogger(AlbCognitoAuthenticationFilter.class);
//
//    private final UserDetailsService userDetailsService;
//    private final AwsCognitoService cognitoService;
//
//    public AlbCognitoAuthenticationFilter(UserDetailsService userDetailsService, AwsCognitoService cognitoService) {
//        this.userDetailsService = userDetailsService;
//        this.cognitoService = cognitoService;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//
//        try {
//            // ALB Cognito 인증 헤더 확인
//            String cognitoUsername = request.getHeader("X-Amzn-Oidc-Identity");
//            String cognitoGroups = request.getHeader("X-Amzn-Oidc-Accesstoken");
//            String cognitoEmail = request.getHeader("X-Amzn-Oidc-Data");
//
//            if (cognitoUsername != null && !cognitoUsername.isEmpty()) {
//                logger.debug("ALB Cognito 인증 사용자 발견: {}", cognitoUsername);
//
//                // 사용자 정보 로드
//                UserDetails userDetails = userDetailsService.loadUserByUsername(cognitoUsername);
//                UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//                logger.debug("ALB Cognito 인증 성공: userId={}", cognitoUsername);
//            }
//
//        } catch (Exception e) {
//            logger.error("ALB Cognito 인증 처리 중 오류 발생", e);
//            // 인증 실패 시에도 요청을 계속 진행
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}
