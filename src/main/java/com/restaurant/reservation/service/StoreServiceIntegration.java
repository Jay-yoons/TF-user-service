package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.ReviewDto;
import com.restaurant.reservation.entity.UserReviewView;
import com.restaurant.reservation.repository.UserReviewViewRepository;
import com.restaurant.reservation.config.MsaConfig;
import com.restaurant.reservation.exception.ServiceConnectionException;
import com.restaurant.reservation.exception.ServiceHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Store Service와의 연동 및 직접 데이터베이스 조회를 처리하는 서비스
 * 
 * @author Team-FOG
 * @version 3.0
 */
@Service
public class StoreServiceIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceIntegration.class);
    
    private final RestTemplate restTemplate;
    private final MsaConfig msaConfig;
    private final UserReviewViewRepository userReviewViewRepository;
    private final JdbcTemplate jdbcTemplate;
    
    public StoreServiceIntegration(RestTemplate restTemplate, MsaConfig msaConfig, UserReviewViewRepository userReviewViewRepository, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.msaConfig = msaConfig;
        this.userReviewViewRepository = userReviewViewRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * 사용자의 리뷰 목록을 뷰를 통해 조회
     * 
     * @param userId 사용자 ID
     * @return 리뷰 목록
     */
    public List<ReviewDto> getUserReviews(String userId) {
        try {
            logger.info("뷰를 통해 사용자 리뷰 조회: userId={}", userId);
            
            // 뷰가 없으면 생성
            createReviewViewIfNotExists();
            
            // 뷰를 통해 리뷰 조회
            List<UserReviewView> reviews = userReviewViewRepository.findByUserId(userId);
            
            // UserReviewView를 ReviewDto로 변환
            List<ReviewDto> reviewDtos = reviews.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            logger.info("뷰를 통한 리뷰 조회 성공: userId={}, count={}", userId, reviewDtos.size());
            
            return reviewDtos;
            
        } catch (Exception e) {
            logger.error("리뷰 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("리뷰 조회 실패", e);
        }
    }
    
    /**
     * 뷰가 존재하지 않으면 생성
     */
    private void createReviewViewIfNotExists() {
        try {
            // 뷰 존재 여부 확인
            String checkViewSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_NAME = 'USER_REVIEW_VIEW'";
            int viewCount = jdbcTemplate.queryForObject(checkViewSql, Integer.class);
            
            if (viewCount == 0) {
                logger.info("USER_REVIEW_VIEW가 존재하지 않습니다. 생성 중...");
                
                // REVIEW와 STORES 테이블 존재 여부 확인
                String checkReviewTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'REVIEW'";
                String checkStoresTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'STORES'";
                
                int reviewTableCount = jdbcTemplate.queryForObject(checkReviewTableSql, Integer.class);
                int storesTableCount = jdbcTemplate.queryForObject(checkStoresTableSql, Integer.class);
                
                if (reviewTableCount > 0 && storesTableCount > 0) {
                    // 뷰 생성
                    String createViewSql = """
                        CREATE VIEW USER_REVIEW_VIEW AS
                        SELECT 
                            r.REVIEW_ID,
                            r.STORE_ID,
                            r.STORE_NAME,
                            r.USER_ID,
                            r.COMENT,
                            r.SCORE,
                            s.STORE_LOCATION,
                            s.CATEGORY_CODE
                        FROM REVIEW r
                        LEFT JOIN STORES s ON r.STORE_ID = s.STORE_ID
                        """;
                    
                    jdbcTemplate.execute(createViewSql);
                    logger.info("USER_REVIEW_VIEW 생성 완료");
                } else {
                    logger.warn("REVIEW 또는 STORES 테이블이 존재하지 않습니다. 뷰 생성을 건너뜁니다.");
                }
            } else {
                logger.debug("USER_REVIEW_VIEW가 이미 존재합니다.");
            }
            
        } catch (Exception e) {
            logger.warn("뷰 생성 중 오류 발생 (무시됨): {}", e.getMessage());
        }
    }
    
    /**
     * UserReviewView를 ReviewDto로 변환
     */
    private ReviewDto convertToDto(UserReviewView view) {
        return new ReviewDto(
            view.getReviewId(),
            view.getStoreId(),
            view.getStoreName(),
            view.getUserId(),
            null, // userName은 별도 조회 필요
            view.getComment(),
            view.getScore()
        );
    }
    
    /**
     * 특정 리뷰의 가게 정보를 Store Service에서 가져오기
     * 
     * @param reviewId 리뷰 ID
     * @return 가게 정보
     */
    public Map<String, Object> getStoreInfoForReview(Long reviewId) {
        try {
            String storeServiceUrl = msaConfig.getServiceUrl("store-service");
            String endpoint = "/api/stores/reviews/" + reviewId + "/store-info";
            String fullUrl = storeServiceUrl + endpoint;
            
            logger.info("Store Service에서 리뷰 관련 가게 정보 조회: reviewId={}", reviewId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> storeInfo = response.getBody();
            logger.info("Store Service에서 가게 정보 조회 성공: reviewId={}", reviewId);
            
            return storeInfo;
            
        } catch (ResourceAccessException e) {
            logger.error("Store Service 연결 실패: {}", e.getMessage());
            throw new ServiceConnectionException("Store Service 연결 실패", e);
        } catch (HttpClientErrorException e) {
            logger.error("Store Service HTTP 오류: HTTP {} - {}", e.getStatusCode(), e.getMessage());
            throw new ServiceHttpException("Store Service HTTP 오류", e);
        } catch (Exception e) {
            logger.error("Store Service 호출 중 예상치 못한 오류: {}", e.getMessage());
            throw new RuntimeException("Store Service 호출 실패", e);
        }
    }
    
    /**
     * 가게 정보를 Store Service에서 가져오기
     * 
     * @param storeId 가게 ID
     * @return 가게 정보
     */
    public Map<String, Object> getStoreInfo(String storeId) {
        try {
            String storeServiceUrl = msaConfig.getServiceUrl("store-service");
            String endpoint = "/api/stores/" + storeId;
            String fullUrl = storeServiceUrl + endpoint;
            
            logger.info("Store Service에서 가게 정보 조회: storeId={}", storeId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> storeInfo = response.getBody();
            logger.info("Store Service에서 가게 정보 조회 성공: storeId={}", storeId);
            
            return storeInfo;
            
        } catch (ResourceAccessException e) {
            logger.error("Store Service 연결 실패: {}", e.getMessage());
            throw new ServiceConnectionException("Store Service 연결 실패", e);
        } catch (HttpClientErrorException e) {
            logger.error("Store Service HTTP 오류: HTTP {} - {}", e.getStatusCode(), e.getMessage());
            throw new ServiceHttpException("Store Service HTTP 오류", e);
        } catch (Exception e) {
            logger.error("Store Service 호출 중 예상치 못한 오류: {}", e.getMessage());
            throw new RuntimeException("Store Service 호출 실패", e);
        }
    }
}
