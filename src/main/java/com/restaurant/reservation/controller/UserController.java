package com.restaurant.reservation.controller;

import com.restaurant.reservation.dto.SignupRequestDto;
import com.restaurant.reservation.dto.SignupResponseDto;
import com.restaurant.reservation.dto.UserInfoDto;
import com.restaurant.reservation.entity.User;
import com.restaurant.reservation.service.UserService;
import com.restaurant.reservation.service.AwsCognitoService;
import com.restaurant.reservation.config.AwsCognitoConfig;
import com.restaurant.reservation.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * User Service 전용 컨트롤러
 * 
 * 사용자 인증/인가 및 사용자 정보 관리 API를 처리합니다.
 * - 회원가입
 * - 로그인/로그아웃
 * - 사용자 정보 관리
 * - JWT 토큰 관리
 * 
 * MSA 원칙에 따라 사용자 인증/인가 중심으로 단순화되었습니다.
 * 
 * @author Team-FOG User Service
 * @version 3.0
 * @since 2025-08-18
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 로깅을 위한 Logger 인스턴스
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    // 사용자 서비스 의존성 주입
    private final UserService userService;
    private final AwsCognitoService cognitoService;
    private final AwsCognitoConfig cognitoConfig;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    
    /**
     * 생성자 - 의존성 주입
     * @param userService 사용자 서비스
     * @param cognitoService AWS Cognito 서비스
     * @param cognitoConfig AWS Cognito 설정
     * @param userDetailsService 사용자 상세 정보 서비스
     */
    public UserController(UserService userService, AwsCognitoService cognitoService, AwsCognitoConfig cognitoConfig,
                         org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        this.userService = userService;
        this.cognitoService = cognitoService;
        this.cognitoConfig = cognitoConfig;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/{id}/name")
    public String getUserName(@PathVariable String id) {
        logger.info("username 컨트롤러 진입");
        String userName = userService.getUserName(id);
        if (userName == null) {
            logger.warn("유저를 찾을 수 없습니다: userId={}", id);
            return "User not found";
        }
        return userName;
    }
    
    /**
     * 통합 마이페이지 조회
     * 사용자 정보 + 통계 + 최근 활동을 한번에 제공
     * 기존 대시보드 기능을 마이페이지에 통합
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyPage() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("통합 마이페이지 조회 요청: userId={}", userId);
            
            Map<String, Object> myPage = userService.getMyPage(userId);
            
            logger.info("통합 마이페이지 조회 완료: userId={}", userId);
            return ResponseEntity.ok(myPage);
            
        } catch (Exception e) {
            logger.error("통합 마이페이지 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 현재 인증된 사용자의 ID를 가져오는 헬퍼 메서드
     */
    private String getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
            
            return null;
        } catch (Exception e) {
            logger.error("현재 사용자 ID 조회 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * Cognito 로그인 URL 생성
     */
    @GetMapping("/login/url")
    public ResponseEntity<Map<String, Object>> generateLoginUrl() {
        try {
            String state = java.util.UUID.randomUUID().toString();
            String loginUrl = cognitoService.generateLoginUrl(state);
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", loginUrl);  // 프론트엔드에서 기대하는 필드명
            response.put("state", state);
            
            logger.info("Cognito 로그인 URL 생성 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("로그인 URL 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    
    /**
     * Cognito 콜백 처리 (인증 코드로 토큰 교환)
     */
    @PostMapping("/login/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(@RequestBody Map<String, String> callbackRequest) {
        try {
            String authorizationCode = callbackRequest.get("code");
            String state = callbackRequest.get("state");
            
            if (authorizationCode == null) {
                logger.warn("인증 코드가 누락되었습니다");
                return ResponseEntity.badRequest().build();
            }
            
            logger.info("Cognito 콜백 처리: code={}, state={}", authorizationCode, state);
            
            // 인증 코드로 토큰 교환
            Map<String, Object> tokenResponse = cognitoService.exchangeCodeForToken(authorizationCode);
            
            // 사용자 정보 추출
            String idToken = (String) tokenResponse.get("id_token");
            Map<String, Object> userInfo = cognitoService.getUserInfoFromIdToken(idToken);
            String userId = (String) userInfo.get("sub");
            
            // 안전한 사용자 정보 추출
            String location = "정보 없음";
            try {
                Object addressObj = userInfo.get("address");
                if (addressObj instanceof Map) {
                    Map<String, Object> address = (Map<String, Object>) addressObj;
                    Object formattedObj = address.get("formatted");
                    if (formattedObj != null) {
                        location = formattedObj.toString();
                    }
                }
            } catch (Exception e) {
                logger.warn("주소 정보 추출 실패, 기본값 사용: {}", e.getMessage());
            }
            
            String name = (String) userInfo.get("name");
            if (name == null) {
                name = "사용자";
                logger.warn("이름 정보가 없어 기본값 사용");
            }
            
            String phoneNumber = (String) userInfo.get("phone_number");
            if (phoneNumber == null) {
                phoneNumber = "정보 없음";
                logger.warn("전화번호 정보가 없어 기본값 사용");
            }

            if (!userService.isUserIdDuplicate(userId)) {
                try {
                    logger.info("회원가입 요청");

                    User user = userService.signup(userId, name, phoneNumber, location);

                    logger.info("회원가입 완료: userId={}", user.getUserId());

                } catch (Exception e) {
                    logger.error("회원가입 중 오류 발생", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            }

            // Spring Security Authentication 객체에 사용자 정보 설정
            try {
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                    userDetailsService.loadUserByUsername(userId);
                
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication = 
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
                
                logger.info("Spring Security 인증 정보 설정 완료: userId={}", userId);
            } catch (Exception authError) {
                logger.warn("Spring Security 인증 정보 설정 실패: userId={}, error={}", userId, authError.getMessage());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accessToken", tokenResponse.get("access_token"));
            response.put("idToken", idToken);
            response.put("refreshToken", tokenResponse.get("refresh_token"));
            response.put("tokenType", "Bearer");
            response.put("expiresIn", tokenResponse.get("expires_in"));
            response.put("userInfo", userInfo);
            response.put("message", "Cognito 로그인 성공");
            
            logger.info("Cognito 로그인 완료: userId={}", userInfo.get("sub"));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Cognito 콜백 처리 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                logger.warn("로그아웃 요청: 인증되지 않은 사용자");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("로그아웃 요청: userId={}", userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그아웃 성공");
            response.put("userId", userId);

            logger.info("로그아웃 완료: userId={}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 전체 사용자 수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getUserCount() {
        try {
            long count = userService.getUserCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("사용자 수 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자 정보 수정
     */
    @PutMapping("/me")
    public ResponseEntity<UserInfoDto> updateMyInfo(@RequestBody Map<String, String> updateRequest) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("사용자 정보 수정 요청: userId={}", userId);

            // 사용자 정보 수정 로직 구현
            User updatedUser = userService.updateUserInfo(userId, updateRequest);
            UserInfoDto userInfo = new UserInfoDto(
                updatedUser.getUserId(), updatedUser.getUserName(), updatedUser.getPhoneNumber(),
                updatedUser.getUserLocation()
            );

            logger.info("사용자 정보 수정 완료: userId={}", userId);
            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            logger.error("사용자 정보 수정 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    
    /**
     * 더미 데이터 생성 (개발용)
     * 프론트엔드 테스트를 위해 비활성화
     */
    // @PostMapping("/dummy/data")
    // public ResponseEntity<Map<String, Object>> createDummyData() {
    //     // 프론트엔드 테스트를 위해 비활성화
    //     Map<String, Object> response = new HashMap<>();
    //     response.put("success", false);
    //     response.put("message", "더미 데이터 생성은 비활성화되었습니다.");
    //     
    //     return ResponseEntity.badRequest().body(response);
    // }








}
