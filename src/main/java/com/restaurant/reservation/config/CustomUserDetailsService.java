package com.restaurant.reservation.config;

import com.restaurant.reservation.entity.User;
import com.restaurant.reservation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * JWT 인증을 위한 사용자 상세 정보 서비스
 * Spring Security의 UserDetailsService를 구현하여 JWT 토큰에서 추출한 userId로 사용자 정보를 로드합니다.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    private final UserRepository userRepository;
    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        logger.debug("사용자 정보 로드 요청: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseGet(() -> {
                    logger.info("새로운 Cognito 사용자 발견: userId={}, 자동 생성 중...", userId);
                    // 새로운 Cognito 사용자를 자동으로 생성
                    return createNewUserFromCognito(userId);
                });
        

        
        // Spring Security UserDetails 객체 생성
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password("N/A") // Cognito 사용자는 로컬 패스워드가 없음
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
        
        logger.debug("사용자 정보 로드 완료: userId={}", userId);
        return userDetails;
    }
    
    /**
     * Cognito에서 로그인한 새로운 사용자를 데이터베이스에 자동 생성
     */
    private User createNewUserFromCognito(String userId) {
        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setUserName("Cognito User");
        newUser.setPhoneNumber("000-0000-0000"); // 임시 전화번호
        newUser.setUserLocation("Unknown");


        
        User savedUser = userRepository.save(newUser);
        logger.info("새로운 Cognito 사용자 생성 완료: userId={}", userId);
        return savedUser;
    }
}
