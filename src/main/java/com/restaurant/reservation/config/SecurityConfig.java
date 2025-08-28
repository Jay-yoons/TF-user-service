package com.restaurant.reservation.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.restaurant.reservation.config.CognitoAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final CognitoAuthenticationFilter cognitoAuthenticationFilter;
//    private final AlbCognitoAuthenticationFilter albCognitoAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> authz
                        // =============================================================================
                        // 1. 인증이 필요 없는 경로들을 한 번에 허용 (가장 먼저 위치해야 함)
                        // =============================================================================
                        .requestMatchers(
                                // 정적 리소스 및 기본 페이지
                                "/", "/login", "/signup", "/register", "/h2-console/**", "/actuator/**", "/public/**",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico", "/.well-known/**",
                                // HTML 페이지
                                "/mypage", "/restaurants", "/reservations", "/reviews",
                                // 공개 API
                                "/api/auth/**", "/api/users/login", "/api/users/login/url", "/api/users/login/callback",
                                "/api/users/count", "/api/users/signup", "/api/users/check/**",
                                "/api/users/dashboard/counts", "/health", "/health/**", "/health/detailed", "/health/log-test", "/api/reviews/{id}", "/api/users/{id}/name",
                                "/login/status", "/login/logout", "/users/count", "/users",
                                // MSA 연동 API
                                "/msa/**", "/api/msa/**", "/api/health/**", "/api/test/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
                .addFilterBefore(cognitoAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://talkingpotato.shop")); // 프론트 주소
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        config.setAllowCredentials(true); // 쿠키 허용 시 true

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
} 