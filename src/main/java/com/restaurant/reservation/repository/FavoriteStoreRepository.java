/*
package com.restaurant.reservation.repository;

import com.restaurant.reservation.entity.FavoriteStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

*/
/**
 * 사용자 즐겨찾기 가게 Repository
 * 
 * 사용자의 즐겨찾기 가게 정보를 데이터베이스에서 조회, 저장, 수정하는 기능을 제공합니다.
 * DB 담당 팀원 피드백에 따라 MSA 원칙을 준수하여 수정되었습니다.
 * 
 * @author FOG Team
 * @version 2.0
 * @since 2025-08-18
 *//*

@Repository
public interface FavoriteStoreRepository extends JpaRepository<FavoriteStore, Long> {
    
    */
/**
     * 특정 사용자의 모든 즐겨찾기 가게 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 즐겨찾기 가게 목록
     *//*

    List<FavoriteStore> findByUserId(String userId);
    
    */
/**
     * 특정 사용자가 특정 가게를 즐겨찾기했는지 확인
     * 
     * @param userId 사용자 ID
     * @param storeId 가게 ID
     * @return 즐겨찾기 정보 (존재하지 않으면 null)
     *//*

    Optional<FavoriteStore> findByUserIdAndStoreId(String userId, String storeId);
    
    */
/**
     * 특정 사용자의 즐겨찾기 가게 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 즐겨찾기 가게 개수
     *//*

    long countByUserId(String userId);
    
    */
/**
     * 특정 사용자의 특정 가게 즐겨찾기 삭제
     * 
     * @param userId 사용자 ID
     * @param storeId 가게 ID
     *//*

    void deleteByUserIdAndStoreId(String userId, String storeId);
    
    */
/**
     * 특정 사용자의 모든 즐겨찾기 삭제
     * 
     * @param userId 사용자 ID
     *//*

    void deleteByUserId(String userId);
    
    // =============================================================================
    // 뷰를 사용한 조회 메서드들 (DB 담당자와 협의 후 추가)
    // =============================================================================
    
    */
/**
     * 사용자의 즐겨찾기 가게 상세 정보 조회 (STORE_NAME 컬럼 사용)
     * DB 담당자 피드백: STORE_NAME 컬럼이 추가되어 JOIN 불필요
     * 
     * @param userId 사용자 ID
     * @return 즐겨찾기 가게 상세 정보 목록 [STORE_ID, STORE_NAME]
     *//*

    @Query(value = "SELECT STORE_ID, STORE_NAME FROM FAV_STORE WHERE USER_ID = :userId", nativeQuery = true)
    List<Object[]> findFavoriteStoresWithDetails(@Param("userId") String userId);
    
    */
/**
     * 사용자의 리뷰 상세 정보 조회 (필요한 컬럼만 조회)
     * DB 담당자 피드백: REVIEW_ID, USER_ID, STORE_ID는 불필요
     * 
     * @param userId 사용자 ID
     * @return 사용자 리뷰 상세 정보 목록 [STORE_NAME, COMMENT, CREATED_AT]
     *//*

    @Query(value = "SELECT STORE_NAME, COMMENT, CREATED_AT FROM V_USER_REVIEWS WHERE USER_ID = :userId ORDER BY REVIEW_ID DESC", nativeQuery = true)
    List<Object[]> findUserReviewsWithDetails(@Param("userId") String userId);
    
    */
/**
     * 사용자의 예약 현황 조회 (뷰 생성 후 사용)
     * DB 담당자 피드백: V_USER_BOOKINGS 뷰 생성 필요
     * 
     * @param userId 사용자 ID
     * @return 사용자 예약 현황 목록 [STORE_NAME, BOOKING_DATE, BOOKING_STATE_NAME]
     *//*

    @Query(value = "SELECT STORE_NAME, BOOKING_DATE, BOOKING_STATE_NAME FROM V_USER_BOOKINGS WHERE USER_ID = :userId ORDER BY BOOKING_DATE DESC", nativeQuery = true)
    List<Object[]> findUserBookingsWithDetails(@Param("userId") String userId);
    
    */
/**
     * 사용자 대시보드 통계 조회 (뷰 생성 후 사용)
     * DB 담당자 피드백: V_USER_DASHBOARD 뷰 생성 필요
     * 
     * @param userId 사용자 ID
     * @return 사용자 대시보드 통계 정보 [TOTAL_FAVORITES, TOTAL_REVIEWS, TOTAL_BOOKINGS]
     *//*

    @Query(value = "SELECT TOTAL_FAVORITES, TOTAL_REVIEWS, TOTAL_BOOKINGS FROM V_USER_DASHBOARD WHERE USER_ID = :userId", nativeQuery = true)
    Object[] findUserDashboardStats(@Param("userId") String userId);
}
*/
