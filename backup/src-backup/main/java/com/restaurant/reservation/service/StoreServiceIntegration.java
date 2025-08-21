package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.ReviewDto;
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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Store Service와의 실제 연동을 처리하는 서비스
 * 
 * 실제 프로덕션 환경에서 Store Service의 API를 호출하여
 * 리뷰 데이터와 가게 정보를 가져옵니다.
 * 
 * @author Team-FOG
 * @version 2.0
 */
@Service
public class StoreServiceIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceIntegration.class);
    
    private final RestTemplate restTemplate;
    private final MsaConfig msaConfig;
    
    public StoreServiceIntegration(RestTemplate restTemplate, MsaConfig msaConfig) {
        this.restTemplate = restTemplate;
        this.msaConfig = msaConfig;
    }
    
    /**
     * 사용자의 리뷰 목록을 Store Service에서 가져오기
     * 
     * @param userId 사용자 ID
     * @return 리뷰 목록
     */
    public List<ReviewDto> getUserReviews(String userId) {
        try {
            String storeServiceUrl = msaConfig.getServiceUrl("store-service");
            String endpoint = "/api/stores/reviews/user/" + userId;
            String fullUrl = storeServiceUrl + endpoint;
            
            logger.info("Store Service에서 사용자 리뷰 조회: userId={}", userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ReviewDto[]> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                entity,
                ReviewDto[].class
            );
            
            List<ReviewDto> reviews = List.of(response.getBody());
            logger.info("Store Service에서 리뷰 조회 성공: userId={}, count={}", userId, reviews.size());
            
            return reviews;
            
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
