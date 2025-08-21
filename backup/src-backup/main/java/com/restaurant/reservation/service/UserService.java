package com.restaurant.reservation.service;

import com.restaurant.reservation.entity.User;
import com.restaurant.reservation.entity.FavoriteStore;
import com.restaurant.reservation.repository.UserRepository;
import com.restaurant.reservation.repository.FavoriteStoreRepository;
import com.restaurant.reservation.dto.FavoriteStoreDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 사용자 서비스 클래스
 * 
 * 이 클래스는 사용자 관련 모든 비즈니스 로직을 처리합니다.
 * - 회원가입 및 로그인 처리
 * - 사용자 정보 관리
 * - 중복 확인 및 유효성 검사
 * 
 * @author FOG Team
 * @version 1.0
 * @since 2024-01-15
 */
@Service
@Transactional // 모든 메서드에 트랜잭션 적용
public class UserService {
    
    // 로깅을 위한 Logger 인스턴스
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    // 사용자 데이터 접근을 위한 Repository
    private final UserRepository userRepository;
    
    // 즐겨찾기 가게 데이터 접근을 위한 Repository
    private final FavoriteStoreRepository favoriteStoreRepository;
    

    
    /**
     * 생성자 - 의존성 주입
     * @param userRepository 사용자 데이터 접근 객체
     * @param favoriteStoreRepository 즐겨찾기 가게 데이터 접근 객체
     */
    public UserService(UserRepository userRepository, FavoriteStoreRepository favoriteStoreRepository) {
        this.userRepository = userRepository;
        this.favoriteStoreRepository = favoriteStoreRepository;
    }
    
    /**
     * 회원가입
     */
    public User signup(String userId, String userName, String phoneNumber, String userLocation) {
        logger.info("회원가입 요청: userId={}, userName={}, phoneNumber={}", userId, userName, phoneNumber);
        
        // 아이디 중복 확인
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        
        // 전화번호 중복 확인
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new RuntimeException("이미 등록된 전화번호입니다.");
        }
        
        // 사용자 생성
        User user = new User();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setPhoneNumber(phoneNumber);
        user.setUserLocation(userLocation);
        
        User savedUser = userRepository.save(user);
        logger.info("회원가입 완료: userId={}", savedUser.getUserId());
        
        return savedUser;
    }
    
    /**
     * 사용자 조회 (Cognito 인증 후 사용)
     */
    public User getUser(String userId) {
        logger.info("사용자 조회: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 사용자입니다."));
        

        
        logger.info("사용자 조회 완료: userId={}", user.getUserId());
        return user;
    }
    
    /**
     * 사용자 이름 조회
     */
    public String getUserName(String userId) {
        logger.info("사용자 이름 조회: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElse(null);
        
        if (user == null) {
            logger.warn("사용자를 찾을 수 없음: userId={}", userId);
            return null;
        }
        
        logger.info("사용자 이름 조회 완료: userId={}, userName={}", userId, user.getUserName());
        return user.getUserName();
    }
    
    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public User getUserInfo(String userId) {
        logger.info("사용자 정보 조회: userId={}", userId);
        
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
    
    /**
     * 사용자 ID 중복 확인
     */
    @Transactional(readOnly = true)
    public boolean isUserIdDuplicate(String userId) {
        return userRepository.existsById(userId);
    }
    
    /**
     * 전화번호 중복 확인
     */
    @Transactional(readOnly = true)
    public boolean isPhoneNumberDuplicate(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    /**
     * 전체 사용자 수 조회
     */
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * 사용자 정보 수정
     */
    public User updateUserInfo(String userId, java.util.Map<String, String> updateRequest) {
        logger.info("사용자 정보 수정 요청: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 업데이트할 필드들 처리
        if (updateRequest.containsKey("userName")) {
            user.setUserName(updateRequest.get("userName"));
        }
        
        if (updateRequest.containsKey("phoneNumber")) {
            String newPhoneNumber = updateRequest.get("phoneNumber");
            // 전화번호 중복 확인 (자신의 전화번호는 제외)
            if (!newPhoneNumber.equals(user.getPhoneNumber()) && 
                userRepository.existsByPhoneNumber(newPhoneNumber)) {
                throw new RuntimeException("이미 등록된 전화번호입니다.");
            }
            user.setPhoneNumber(newPhoneNumber);
        }
        
        if (updateRequest.containsKey("userLocation")) {
            user.setUserLocation(updateRequest.get("userLocation"));
        }
        


        
        User updatedUser = userRepository.save(user);
        logger.info("사용자 정보 수정 완료: userId={}", updatedUser.getUserId());
        
        return updatedUser;
    }

    // =============================================================================
    // 즐겨찾기 가게 관련 메서드
    // =============================================================================

    /**
     * 사용자의 즐겨찾기 가게 목록 조회 (기존 방식 - 더미 데이터 사용)
     */
    @Transactional(readOnly = true)
    public List<FavoriteStoreDto> getFavoriteStores(String userId) {
        logger.info("즐겨찾기 가게 목록 조회: userId={}", userId);
        
        List<FavoriteStore> favoriteStores = favoriteStoreRepository.findByUserId(userId);
        List<FavoriteStoreDto> favoriteStoreDtos = new ArrayList<>();
        
        for (FavoriteStore favoriteStore : favoriteStores) {
            // 실제로는 Store Service에서 가게 이름을 가져와야 함
            // 현재는 더미 데이터로 응답
            String storeName = getDummyStoreName(favoriteStore.getStoreId());
            
            FavoriteStoreDto dto = new FavoriteStoreDto(
                favoriteStore.getFavStoreId(),
                favoriteStore.getStoreId(),
                storeName,
                favoriteStore.getUserId()
            );
            favoriteStoreDtos.add(dto);
        }
        
        logger.info("즐겨찾기 가게 목록 조회 완료: userId={}, count={}", userId, favoriteStoreDtos.size());
        return favoriteStoreDtos;
    }

    /**
     * 뷰를 사용하여 사용자의 즐겨찾기 가게 상세 정보 조회 (개선된 방식)
     * DB 담당자와 협의 후 사용
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFavoriteStoresWithDetails(String userId) {
        logger.info("뷰를 사용한 즐겨찾기 가게 상세 정보 조회: userId={}", userId);
        
        try {
            List<Object[]> results = favoriteStoreRepository.findFavoriteStoresWithDetails(userId);
            List<Map<String, Object>> favoriteStores = new ArrayList<>();
            
            for (Object[] row : results) {
                Map<String, Object> store = new HashMap<>();
                store.put("favStoreId", row[0]);
                store.put("userId", row[1]);
                store.put("storeId", row[2]);
                store.put("storeName", row[3]);
                store.put("storeLocation", row[4]);
                store.put("serviceTime", row[5]);
                store.put("categoryCode", row[6]);
                store.put("categoryName", row[7]);
                store.put("seatNum", row[8]);
                favoriteStores.add(store);
            }
            
            logger.info("뷰를 사용한 즐겨찾기 가게 조회 완료: userId={}, count={}", userId, favoriteStores.size());
            return favoriteStores;
            
        } catch (Exception e) {
            logger.error("뷰 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            // 뷰가 없거나 오류 발생 시 기존 방식으로 fallback
            logger.info("기존 방식으로 fallback: userId={}", userId);
            return getFavoriteStoresFallback(userId);
        }
    }

    /**
     * 뷰를 사용하여 사용자의 리뷰 상세 정보 조회
     * DB 담당자와 협의 후 사용
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserReviewsWithDetails(String userId) {
        logger.info("뷰를 사용한 사용자 리뷰 상세 정보 조회: userId={}", userId);
        
        try {
            List<Object[]> results = favoriteStoreRepository.findUserReviewsWithDetails(userId);
            List<Map<String, Object>> reviews = new ArrayList<>();
            
            for (Object[] row : results) {
                Map<String, Object> review = new HashMap<>();
                review.put("reviewId", row[0]);
                review.put("userId", row[1]);
                review.put("storeId", row[2]);
                review.put("storeName", row[3]);
                review.put("storeLocation", row[4]);
                review.put("categoryCode", row[5]);
                review.put("categoryName", row[6]);
                review.put("comment", row[7]);
                review.put("score", row[8]);
                review.put("reviewIdStr", row[9]);
                reviews.add(review);
            }
            
            logger.info("뷰를 사용한 리뷰 조회 완료: userId={}, count={}", userId, reviews.size());
            return reviews;
            
        } catch (Exception e) {
            logger.error("뷰 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            // 뷰가 없거나 오류 발생 시 기존 방식으로 fallback
            logger.info("기존 방식으로 fallback: userId={}", userId);
            return getUserReviewsFallback(userId);
        }
    }

    /**
     * 뷰를 사용하여 사용자의 예약 현황 조회
     * DB 담당자와 협의 후 사용
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserBookingsWithDetails(String userId) {
        logger.info("뷰를 사용한 사용자 예약 현황 조회: userId={}", userId);
        
        try {
            List<Object[]> results = favoriteStoreRepository.findUserBookingsWithDetails(userId);
            List<Map<String, Object>> bookings = new ArrayList<>();
            
            for (Object[] row : results) {
                Map<String, Object> booking = new HashMap<>();
                booking.put("bookingNum", row[0]);
                booking.put("userId", row[1]);
                booking.put("storeId", row[2]);
                booking.put("storeName", row[3]);
                booking.put("storeLocation", row[4]);
                booking.put("categoryCode", row[5]);
                booking.put("categoryName", row[6]);
                booking.put("bookingDate", row[7]);
                booking.put("bookingStateCode", row[8]);
                booking.put("stateName", row[9]);
                booking.put("count", row[10]);
                booking.put("inUsingSeat", row[11]);
                booking.put("seatNum", row[12]);
                bookings.add(booking);
            }
            
            logger.info("뷰를 사용한 예약 조회 완료: userId={}, count={}", userId, bookings.size());
            return bookings;
            
        } catch (Exception e) {
            logger.error("뷰 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            // 뷰가 없거나 오류 발생 시 빈 리스트 반환
            return new ArrayList<>();
        }
    }

    /**
     * 뷰를 사용하여 사용자 대시보드 통계 조회
     * DB 담당자와 협의 후 사용
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserDashboardStats(String userId) {
        logger.info("뷰를 사용한 사용자 대시보드 통계 조회: userId={}", userId);
        
        try {
            Object[] result = favoriteStoreRepository.findUserDashboardStats(userId);
            Map<String, Object> dashboard = new HashMap<>();
            
            if (result != null) {
                dashboard.put("userId", result[0]);
                dashboard.put("userName", result[1]);
                dashboard.put("phoneNumber", result[2]);
                dashboard.put("userLocation", result[3]);
                dashboard.put("favoriteCount", result[4]);
                dashboard.put("reviewCount", result[5]);
                dashboard.put("avgReviewScore", result[6]);
                dashboard.put("totalBookingCount", result[7]);
                dashboard.put("activeBookingCount", result[8]);
                dashboard.put("cancelledBookingCount", result[9]);
            }
            
            logger.info("뷰를 사용한 대시보드 통계 조회 완료: userId={}", userId);
            return dashboard;
            
        } catch (Exception e) {
            logger.error("뷰 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            // 뷰가 없거나 오류 발생 시 기본 정보만 반환
            return getUserDashboardStatsFallback(userId);
        }
    }

    // =============================================================================
    // Fallback 메서드들 (뷰가 없을 때 사용)
    // =============================================================================

    /**
     * 즐겨찾기 가게 조회 fallback (기존 방식)
     */
    private List<Map<String, Object>> getFavoriteStoresFallback(String userId) {
        List<FavoriteStore> favoriteStores = favoriteStoreRepository.findByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (FavoriteStore fs : favoriteStores) {
            Map<String, Object> store = new HashMap<>();
            store.put("favStoreId", fs.getFavStoreId());
            store.put("userId", fs.getUserId());
            store.put("storeId", fs.getStoreId());
            store.put("storeName", getDummyStoreName(fs.getStoreId()));
            store.put("storeLocation", "주소 정보 없음");
            store.put("serviceTime", "영업시간 정보 없음");
            store.put("categoryCode", "UNKNOWN");
            store.put("categoryName", "알 수 없음");
            store.put("seatNum", 0);

            result.add(store);
        }
        
        return result;
    }

    /**
     * 사용자 리뷰 조회 fallback (기존 방식)
     */
    private List<Map<String, Object>> getUserReviewsFallback(String userId) {
        // Store Service Integration을 사용하여 리뷰 조회
        try {
            // StoreServiceIntegration을 주입받아야 함
            // 현재는 빈 리스트 반환
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("리뷰 조회 fallback 실패: userId={}, error={}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 사용자 대시보드 통계 fallback (기존 방식)
     */
    private Map<String, Object> getUserDashboardStatsFallback(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        Map<String, Object> dashboard = new HashMap<>();
        
        if (user != null) {
            dashboard.put("userId", user.getUserId());
            dashboard.put("userName", user.getUserName());
            dashboard.put("phoneNumber", user.getPhoneNumber());
            dashboard.put("userLocation", user.getUserLocation());

            dashboard.put("favoriteCount", favoriteStoreRepository.countByUserId(userId));
            dashboard.put("reviewCount", 0); // Store Service 연동 필요
            dashboard.put("avgReviewScore", 0.0);
            dashboard.put("totalBookingCount", 0); // Reservation Service 연동 필요
            dashboard.put("activeBookingCount", 0);
            dashboard.put("cancelledBookingCount", 0);
        }
        
        return dashboard;
    }

    /**
     * 즐겨찾기 가게 추가
     */
    public FavoriteStoreDto addFavoriteStore(String userId, String storeId) {
        logger.info("즐겨찾기 가게 추가: userId={}, storeId={}", userId, storeId);
        
        // 이미 즐겨찾기한 가게인지 확인
        if (favoriteStoreRepository.findByUserIdAndStoreId(userId, storeId).isPresent()) {
            throw new RuntimeException("이미 즐겨찾기한 가게입니다.");
        }
        
        // 즐겨찾기 가게 생성
        FavoriteStore favoriteStore = new FavoriteStore(userId, storeId);
        FavoriteStore savedFavoriteStore = favoriteStoreRepository.save(favoriteStore);
        
        // 실제로는 Store Service에서 가게 이름을 가져와야 함
        String storeName = getDummyStoreName(storeId);
        
        FavoriteStoreDto dto = new FavoriteStoreDto(
            savedFavoriteStore.getFavStoreId(),
            savedFavoriteStore.getStoreId(),
            storeName,
            savedFavoriteStore.getUserId()
        );
        
        logger.info("즐겨찾기 가게 추가 완료: userId={}, storeId={}", userId, storeId);
        return dto;
    }

    /**
     * 즐겨찾기 가게 삭제
     */
    public void removeFavoriteStore(String userId, String storeId) {
        logger.info("즐겨찾기 가게 삭제: userId={}, storeId={}", userId, storeId);
        
        favoriteStoreRepository.deleteByUserIdAndStoreId(userId, storeId);
        
        logger.info("즐겨찾기 가게 삭제 완료: userId={}, storeId={}", userId, storeId);
    }

    /**
     * 특정 가게가 즐겨찾기되어 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isFavoriteStore(String userId, String storeId) {
        return favoriteStoreRepository.findByUserIdAndStoreId(userId, storeId).isPresent();
    }

    /**
     * 사용자의 즐겨찾기 가게 개수 조회
     */
    @Transactional(readOnly = true)
    public long getFavoriteStoreCount(String userId) {
        return favoriteStoreRepository.countByUserId(userId);
    }

    /**
     * 더미 가게 이름 생성 (실제로는 Store Service에서 가져와야 함)
     */
    private String getDummyStoreName(String storeId) {
        switch (storeId) {
            case "store001":
                return "맛있는 한식당";
            case "store002":
                return "신선한 중식당";
            case "store003":
                return "분위기 좋은 카페";
            case "store004":
                return "고급 양식당";
            case "store005":
                return "전통 일본식당";
            default:
                return "알 수 없는 가게";
        }
    }

    /**
     * 통합 마이페이지 정보 조회
     * 사용자 정보 + 통계 + 최근 활동을 한번에 제공
     * 기존 대시보드 기능을 마이페이지에 통합
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMyPage(String userId) {
        logger.info("통합 마이페이지 정보 조회: userId={}", userId);
        
        Map<String, Object> myPage = new HashMap<>();
        
        try {
            // 1. 사용자 기본 정보
            User user = userRepository.findById(userId).orElse(null);
            Map<String, Object> userInfo = new HashMap<>();
            if (user != null) {
                userInfo.put("userId", user.getUserId());
                userInfo.put("userName", user.getUserName());
                userInfo.put("phoneNumber", user.getPhoneNumber());
                userInfo.put("userLocation", user.getUserLocation());

            }
            myPage.put("userInfo", userInfo);
            
            // 2. 통계 정보
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("favoriteCount", favoriteStoreRepository.countByUserId(userId));
            statistics.put("reviewCount", getUserReviewCount(userId));
            statistics.put("totalBookingCount", getUserBookingCount(userId));
            statistics.put("activeBookingCount", getActiveBookingCount(userId));
            myPage.put("statistics", statistics);
            
            // 3. 최근 활동 (최근 5개씩)
            Map<String, Object> recentActivities = new HashMap<>();
            recentActivities.put("favorites", getRecentFavorites(userId, 5));
            recentActivities.put("reviews", getRecentReviews(userId, 5));
            recentActivities.put("bookings", getRecentBookings(userId, 5));
            myPage.put("recentActivities", recentActivities);
            
            logger.info("통합 마이페이지 정보 조회 완료: userId={}", userId);
            return myPage;
            
        } catch (Exception e) {
            logger.error("통합 마이페이지 정보 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            // 오류 발생 시 기본 정보만 반환
            return getMyPageFallback(userId);
        }
    }
    
    /**
     * 최근 즐겨찾기 조회
     */
    private List<Map<String, Object>> getRecentFavorites(String userId, int limit) {
        List<FavoriteStore> favorites = favoriteStoreRepository.findByUserId(userId);
        return favorites.stream()
            .limit(limit)
            .map(fs -> {
                Map<String, Object> fav = new HashMap<>();
                fav.put("storeId", fs.getStoreId());
                fav.put("storeName", fs.getStoreName() != null ? fs.getStoreName() : getDummyStoreName(fs.getStoreId()));

                return fav;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 최근 리뷰 조회
     */
    private List<Map<String, Object>> getRecentReviews(String userId, int limit) {
        // Store Service Integration을 사용하여 리뷰 조회
        try {
            // 현재는 빈 리스트 반환 (Store Service 연동 후 구현)
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("최근 리뷰 조회 실패: userId={}, error={}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 최근 예약 조회
     */
    private List<Map<String, Object>> getRecentBookings(String userId, int limit) {
        // Reservation Service Integration을 사용하여 예약 조회
        try {
            // 현재는 빈 리스트 반환 (Reservation Service 연동 후 구현)
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("최근 예약 조회 실패: userId={}, error={}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 사용자 리뷰 개수 조회
     */
    private int getUserReviewCount(String userId) {
        // Store Service Integration을 사용하여 리뷰 개수 조회
        try {
            // 현재는 0 반환 (Store Service 연동 후 구현)
            return 0;
        } catch (Exception e) {
            logger.error("리뷰 개수 조회 실패: userId={}, error={}", userId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 사용자 예약 개수 조회
     */
    private int getUserBookingCount(String userId) {
        // Reservation Service Integration을 사용하여 예약 개수 조회
        try {
            // 현재는 0 반환 (Reservation Service 연동 후 구현)
            return 0;
        } catch (Exception e) {
            logger.error("예약 개수 조회 실패: userId={}, error={}", userId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 활성 예약 개수 조회
     */
    private int getActiveBookingCount(String userId) {
        // Reservation Service Integration을 사용하여 활성 예약 개수 조회
        try {
            // 현재는 0 반환 (Reservation Service 연동 후 구현)
            return 0;
        } catch (Exception e) {
            logger.error("활성 예약 개수 조회 실패: userId={}, error={}", userId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 통합 마이페이지 fallback (기본 정보만 반환)
     */
    private Map<String, Object> getMyPageFallback(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        Map<String, Object> myPage = new HashMap<>();
        
        // 사용자 기본 정보
        Map<String, Object> userInfo = new HashMap<>();
        if (user != null) {
            userInfo.put("userId", user.getUserId());
            userInfo.put("userName", user.getUserName());
            userInfo.put("phoneNumber", user.getPhoneNumber());
            userInfo.put("userLocation", user.getUserLocation());

        }
        myPage.put("userInfo", userInfo);
        
        // 기본 통계 정보
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("favoriteCount", favoriteStoreRepository.countByUserId(userId));
        statistics.put("reviewCount", 0);
        statistics.put("totalBookingCount", 0);
        statistics.put("activeBookingCount", 0);
        myPage.put("statistics", statistics);
        
        // 빈 최근 활동
        Map<String, Object> recentActivities = new HashMap<>();
        recentActivities.put("favorites", new ArrayList<>());
        recentActivities.put("reviews", new ArrayList<>());
        recentActivities.put("bookings", new ArrayList<>());
        myPage.put("recentActivities", recentActivities);
        
        return myPage;
    }
}
