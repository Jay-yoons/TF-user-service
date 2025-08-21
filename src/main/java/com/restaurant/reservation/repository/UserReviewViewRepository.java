package com.restaurant.reservation.repository;

import com.restaurant.reservation.entity.UserReviewView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserReviewView 전용 Repository
 * 조회 전용으로만 사용 (Read Only)
 */
@Repository
public interface UserReviewViewRepository extends JpaRepository<UserReviewView, Long> {
    
    /**
     * 사용자 ID로 리뷰 목록 조회
     */
    List<UserReviewView> findByUserId(String userId);
    
    /**
     * 매장 ID로 리뷰 목록 조회
     */
    List<UserReviewView> findByStoreId(String storeId);
    
    /**
     * 리뷰 ID와 사용자 ID로 특정 리뷰 조회
     */
    Optional<UserReviewView> findByReviewIdAndUserId(Long reviewId, String userId);
    
    /**
     * 사용자 ID로 리뷰 개수 조회
     */
    long countByUserId(String userId);
}
