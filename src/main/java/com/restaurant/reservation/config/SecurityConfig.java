package com.restaurant.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.restaurant.reservation.config.CognitoAuthenticationFilter;
import com.restaurant.reservation.config.AlbCognitoAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CognitoAuthenticationFilter cognitoAuthenticationFilter;
    private final AlbCognitoAuthenticationFilter albCognitoAuthenticationFilter;

    public SecurityConfig(CognitoAuthenticationFilter cognitoAuthenticationFilter, 
                         AlbCognitoAuthenticationFilter albCognitoAuthenticationFilter) {
        this.cognitoAuthenticationFilter = cognitoAuthenticationFilter;
        this.albCognitoAuthenticationFilter = albCognitoAuthenticationFilter;
    }

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
                // 정적 리소스 및 기본 페이지 (인증 불필요)
                // =============================================================================
                .requestMatchers("/", "/login", "/signup", "/register", "/h2-console/**", "/actuator/**", "/public/**", 
                               "/css/**", "/js/**", "/images/**", "/favicon.ico", "/.well-known/**").permitAll()
                
                // =============================================================================
                // HTML 페이지 (인증 불필요)
                // =============================================================================
                .requestMatchers("/mypage", "/restaurants", "/reservations", "/reviews").permitAll()
                
                // =============================================================================
                // 공개 API (인증 불필요)
                // =============================================================================
                .requestMatchers("/login/status", "/login/logout", "/users/count", "/users", "/api/auth/**", 
                               "/api/users/login", "/api/users/login/url", "/api/users/login/callback", 
                               "/api/users/count", "/api/users/signup", "/api/users/check/**",
                               "/api/users/dashboard/counts", "/health", "/api/reviews/{id}", "/api/users/{id}/name").permitAll()
                
                // =============================================================================
                // MSA 연동 API (인증 불필요)
                // =============================================================================
                .requestMatchers("/msa/**", "/api/msa/**", "/api/health/**", "/api/test/**").permitAll()
                
                // =============================================================================
                // 사용자 관련 API (JWT 토큰 필요)
                // =============================================================================
                .requestMatchers("/users/me", "/favorites/**", "/reviews/**", "/bookings/**").authenticated()
                
                // =============================================================================
                // 기타 API (JWT 토큰 필요)
                // =============================================================================
                .requestMatchers("/api/**").authenticated()
                
                // =============================================================================
                // 기타 모든 요청 (인증 필요)
                // =============================================================================
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .addFilterBefore(albCognitoAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(cognitoAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://talkingpotato.shop")); // 프론트 주소
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키 허용 시 true

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
} 