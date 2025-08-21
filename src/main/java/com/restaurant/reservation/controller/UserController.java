package com.restaurant.reservation.controller;

import com.restaurant.reservation.dto.SignupRequestDto;
import com.restaurant.reservation.dto.SignupResponseDto;
import com.restaurant.reservation.dto.UserInfoDto;
import com.restaurant.reservation.dto.ReviewDto;
//import com.restaurant.reservation.dto.FavoriteStoreDto;
import com.restaurant.reservation.entity.User;
import com.restaurant.reservation.service.UserService;
import com.restaurant.reservation.service.AwsCognitoService;
import com.restaurant.reservation.service.StoreServiceIntegration;
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
import java.util.List;
import java.util.ArrayList;

/**
 * User Service 전용 컨트롤러
 * 
 * 사용자 관련 모든 API를 처리합니다.
 * - 회원가입
 * - 로그인/로그아웃
 * - 사용자 정보 관리
 * - JWT 토큰 관리
 * 
 * @author Team-FOG User Service
 * @version 2.0
 * @since 2025-08-18
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {
    "http://localhost:3000",  // Vue.js 개발 서버
    "http://localhost:8080",  // Vite 개발 서버
    "http://localhost:8081",  // 추가 개발 서버
    "http://localhost:8082",  // User Service 자체
    "http://localhost:5173",  // Vite 기본 포트
    "http://localhost:4173",  // Vite 프리뷰 포트
    "http://127.0.0.1:3000",  // IP 주소
    "http://127.0.0.1:8080",
    "http://127.0.0.1:8081",
    "http://127.0.0.1:8082",
    "http://127.0.0.1:5173",
    "http://127.0.0.1:4173"
}, allowCredentials = "true")
public class UserController {
    
    // 로깅을 위한 Logger 인스턴스
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    // 사용자 서비스 의존성 주입
    private final UserService userService;
    private final AwsCognitoService cognitoService;
    private final AwsCognitoConfig cognitoConfig;
    private final StoreServiceIntegration storeServiceIntegration;
    
    /**
     * 생성자 - 의존성 주입
     * @param userService 사용자 서비스
     * @param cognitoService AWS Cognito 서비스
     * @param cognitoConfig AWS Cognito 설정
     * @param storeServiceIntegration Store Service 연동 서비스
     */
    public UserController(UserService userService, AwsCognitoService cognitoService, AwsCognitoConfig cognitoConfig, StoreServiceIntegration storeServiceIntegration) {
        this.userService = userService;
        this.cognitoService = cognitoService;
        this.cognitoConfig = cognitoConfig;
        this.storeServiceIntegration = storeServiceIntegration;
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
            String location = ((Map<String, Object>) userInfo.get("address")).get("formatted").toString();
            String name = (String) userInfo.get("name");
            String phoneNumber = (String) userInfo.get("phone_number");

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
     * 서비스 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
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

    /**
     * 마이페이지 - 내가 작성한 리뷰 목록 조회
     */
    @GetMapping("/me/reviews")
    public ResponseEntity<List<ReviewDto>> getMyReviews() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("내 리뷰 목록 조회 요청: userId={}", userId);

            // Store Service에서 리뷰 데이터를 가져오기
            List<ReviewDto> reviews = storeServiceIntegration.getUserReviews(userId);

            logger.info("내 리뷰 목록 조회 완료: userId={}, count={}", userId, reviews.size());
            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            logger.error("내 리뷰 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 특정 가게의 리뷰 페이지로 이동하기 위한 정보 조회
     */
    @GetMapping("/me/reviews/{reviewId}/store-info")
    public ResponseEntity<Map<String, Object>> getStoreInfoForReview(@PathVariable Long reviewId) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("리뷰 관련 가게 정보 조회 요청: userId={}, reviewId={}", userId, reviewId);

            // Store Service에서 가게 정보를 가져오기
            Map<String, Object> storeInfo = storeServiceIntegration.getStoreInfoForReview(reviewId);

            logger.info("리뷰 관련 가게 정보 조회 완료: reviewId={}", reviewId);
            return ResponseEntity.ok(storeInfo);

        } catch (Exception e) {
            logger.error("리뷰 관련 가게 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



/*    // =============================================================================
    // 즐겨찾기 가게 관련 API
    // =============================================================================

    *//**
     * 마이페이지 - 내 즐겨찾기 가게 목록 조회
     *//*
    @GetMapping("/me/favorites")
    public ResponseEntity<List<FavoriteStoreDto>> getMyFavorites() {
        try {
            String userId = getCurrentUserId();
            logger.info("내 즐겨찾기 가게 목록 조회 요청: userId={}", userId);

            List<FavoriteStoreDto> favorites = userService.getFavoriteStores(userId);
            
            logger.info("내 즐겨찾기 가게 목록 조회 완료: userId={}, count={}", userId, favorites.size());
            return ResponseEntity.ok(favorites);

        } catch (Exception e) {
            logger.error("내 즐겨찾기 가게 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    *//**
     * 뷰를 사용한 내 즐겨찾기 가게 상세 정보 조회 (개선된 방식)
     * DB 담당자와 협의 후 사용
     *//*
    @GetMapping("/me/favorites/details")
    public ResponseEntity<List<Map<String, Object>>> getMyFavoritesWithDetails() {
        try {
            String userId = getCurrentUserId();
            logger.info("뷰를 사용한 내 즐겨찾기 가게 상세 정보 조회 요청: userId={}", userId);

            List<Map<String, Object>> favorites = userService.getFavoriteStoresWithDetails(userId);
            
            logger.info("뷰를 사용한 내 즐겨찾기 가게 상세 정보 조회 완료: userId={}, count={}", userId, favorites.size());
            return ResponseEntity.ok(favorites);

        } catch (Exception e) {
            logger.error("뷰를 사용한 내 즐겨찾기 가게 상세 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    *//**
     * 즐겨찾기 가게 추가
     *//*
    @PostMapping("/me/favorites")
    public ResponseEntity<FavoriteStoreDto> addFavoriteStore(@RequestBody Map<String, String> request) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String storeId = request.get("storeId");
            if (storeId == null || storeId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            String storeName = request.get("storeName");

            logger.info("즐겨찾기 가게 추가 요청: userId={}, storeId={}, storeName={}", userId, storeId, storeName);

            FavoriteStoreDto favoriteStore = userService.addFavoriteStore(userId, storeId, storeName);

            logger.info("즐겨찾기 가게 추가 완료: userId={}, storeId={}", userId, storeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(favoriteStore);

        } catch (Exception e) {
            logger.error("즐겨찾기 가게 추가 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    *//**
     * 즐겨찾기 가게 삭제
     *//*
    @DeleteMapping("/me/favorites/{storeId}")
    public ResponseEntity<Map<String, Object>> removeFavoriteStore(@PathVariable String storeId) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("즐겨찾기 가게 삭제 요청: userId={}, storeId={}", userId, storeId);

            userService.removeFavoriteStore(userId, storeId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "즐겨찾기 가게가 삭제되었습니다.");

            logger.info("즐겨찾기 가게 삭제 완료: userId={}, storeId={}", userId, storeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("즐겨찾기 가게 삭제 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    *//**
     * 특정 가게의 즐겨찾기 상태 확인
     *//*
    @GetMapping("/me/favorites/{storeId}/check")
    public ResponseEntity<Map<String, Object>> checkFavoriteStore(@PathVariable String storeId) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("즐겨찾기 상태 확인 요청: userId={}, storeId={}", userId, storeId);

            boolean isFavorite = userService.isFavoriteStore(userId, storeId);

            Map<String, Object> response = new HashMap<>();
            response.put("isFavorite", isFavorite);
            response.put("storeId", storeId);

            logger.info("즐겨찾기 상태 확인 완료: userId={}, storeId={}, isFavorite={}", userId, storeId, isFavorite);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("즐겨찾기 상태 확인 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    *//**
     * 내 즐겨찾기 가게 개수 조회 (기존 API - 통합 마이페이지로 대체됨)
     * @deprecated 통합 마이페이지 API 사용 권장
     *//*
    @GetMapping("/me/favorites/count")
    @Deprecated
    public ResponseEntity<Map<String, Object>> getMyFavoriteStoreCount() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("즐겨찾기 가게 개수 조회 요청: userId={}", userId);

            long count = userService.getFavoriteStoreCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);

            logger.info("즐겨찾기 가게 개수 조회 완료: userId={}, count={}", userId, count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("즐겨찾기 가게 개수 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }*/

 /*   *//**
     * 뷰를 사용한 내 리뷰 상세 정보 조회 (기존 API - 통합 마이페이지로 대체됨)
     * @deprecated 통합 마이페이지 API 사용 권장
     *//*
    @GetMapping("/me/reviews/details")
    @Deprecated
    public ResponseEntity<List<Map<String, Object>>> getMyReviewsWithDetails() {
        try {
            String userId = getCurrentUserId();
            logger.info("뷰를 사용한 내 리뷰 상세 정보 조회 요청: userId={}", userId);

            List<Map<String, Object>> reviews = userService.getUserReviewsWithDetails(userId);
            
            logger.info("뷰를 사용한 내 리뷰 상세 정보 조회 완료: userId={}, count={}", userId, reviews.size());
            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            logger.error("뷰를 사용한 내 리뷰 상세 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    *//**
     * 뷰를 사용한 내 예약 현황 조회 (기존 API - 통합 마이페이지로 대체됨)
     * @deprecated 통합 마이페이지 API 사용 권장
     *//*
    @GetMapping("/me/bookings/details")
    @Deprecated
    public ResponseEntity<List<Map<String, Object>>> getMyBookingsWithDetails() {
        try {
            String userId = getCurrentUserId();
            logger.info("뷰를 사용한 내 예약 현황 조회 요청: userId={}", userId);

            List<Map<String, Object>> bookings = userService.getUserBookingsWithDetails(userId);
            
            logger.info("뷰를 사용한 내 예약 현황 조회 완료: userId={}, count={}", userId, bookings.size());
            return ResponseEntity.ok(bookings);

        } catch (Exception e) {
            logger.error("뷰를 사용한 내 예약 현황 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    *//**
     * 뷰를 사용한 내 대시보드 통계 조회 (기존 API - 통합 마이페이지로 대체됨)
     * @deprecated 통합 마이페이지 API 사용 권장
     *//*
    @GetMapping("/me/dashboard/stats")
    @Deprecated
    public ResponseEntity<Map<String, Object>> getMyDashboardStats() {
        try {
            String userId = getCurrentUserId();
            logger.info("뷰를 사용한 내 대시보드 통계 조회 요청: userId={}", userId);

            Map<String, Object> dashboardStats = userService.getUserDashboardStats(userId);
            
            logger.info("뷰를 사용한 내 대시보드 통계 조회 완료: userId={}", userId);
            return ResponseEntity.ok(dashboardStats);

        } catch (Exception e) {
            logger.error("뷰를 사용한 내 대시보드 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }*/
}
