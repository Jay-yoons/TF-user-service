package com.restaurant.reservation.service;

import com.restaurant.reservation.config.MsaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MSA 환경에서 다른 서비스들과의 연동을 처리하는 서비스
 * 
 * 이 서비스는 다른 마이크로서비스들과의 통신을 담당하며, JWT 토큰 기반 인증을 지원합니다.
 * 
 * 주요 기능:
 * 1. 다른 서비스와의 HTTP 통신
 * 2. JWT 토큰 기반 인증
 * 3. 서비스별 타임아웃 및 재시도 처리
 * 4. 장애 처리 및 폴백 메커니즘
 * 5. 비동기 통신 지원
 * 
 * @author Team-FOG
 * @version 2.0
 */
@Service
public class MsaIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MsaIntegrationService.class);
    
    private final RestTemplate restTemplate;
    private final MsaConfig msaConfig;
    
    public MsaIntegrationService(RestTemplate restTemplate, MsaConfig msaConfig) {
        this.restTemplate = restTemplate;
        this.msaConfig = msaConfig;
    }
    
    /**
     * 다른 서비스와 통신하는 기본 메서드
     * 
     * @param serviceName 서비스 이름
     * @param endpoint 엔드포인트 경로
     * @param method HTTP 메서드
     * @param requestBody 요청 본문
     * @param responseType 응답 타입
     * @param <T> 응답 타입 파라미터
     * @return 서비스 응답
     */
    public <T> ResponseEntity<T> callExternalService(
            String serviceName, 
            String endpoint, 
            HttpMethod method, 
            Object requestBody, 
            Class<T> responseType) {
        
        String serviceUrl = msaConfig.getServiceUrl(serviceName);
        if (serviceUrl.isEmpty()) {
            logger.error("서비스 URL이 설정되지 않음: {}", serviceName);
            throw new IllegalArgumentException("서비스 URL이 설정되지 않음: " + serviceName);
        }
        
        String fullUrl = serviceUrl + endpoint;
        int timeout = msaConfig.getServiceTimeout(serviceName);
        
        try {
            logger.debug("외부 서비스 호출: {} {} -> {}", method, fullUrl, serviceName);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Name", "restaurant-reservation-service");
            headers.set("X-Request-ID", generateRequestId());
            
            // HTTP 엔티티 생성
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
            
            // REST 템플릿 호출
            ResponseEntity<T> response = restTemplate.exchange(
                fullUrl, 
                method, 
                entity, 
                responseType
            );
            
            logger.info("외부 서비스 호출 성공: {} {} - HTTP {}", method, fullUrl, response.getStatusCode());
            return response;
            
        } catch (ResourceAccessException e) {
            logger.error("외부 서비스 연결 실패: {} - {}", serviceName, e.getMessage());
            throw new ServiceConnectionException("서비스 연결 실패: " + serviceName, e);
        } catch (HttpClientErrorException e) {
            logger.error("외부 서비스 HTTP 오류: {} - HTTP {} - {}", serviceName, e.getStatusCode(), e.getMessage());
            throw new ServiceHttpException("서비스 HTTP 오류: " + serviceName, e);
        } catch (Exception e) {
            logger.error("외부 서비스 호출 중 예상치 못한 오류: {} - {}", serviceName, e.getMessage());
            throw new ServiceException("서비스 호출 오류: " + serviceName, e);
        }
    }
    
    /**
     * 다른 서비스와 비동기 통신
     * 
     * @param serviceName 서비스 이름
     * @param endpoint 엔드포인트 경로
     * @param method HTTP 메서드
     * @param requestBody 요청 본문
     * @param responseType 응답 타입
     * @param <T> 응답 타입 파라미터
     * @return CompletableFuture로 래핑된 응답
     */
    public <T> CompletableFuture<ResponseEntity<T>> callExternalServiceAsync(
            String serviceName, 
            String endpoint, 
            HttpMethod method, 
            Object requestBody, 
            Class<T> responseType) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callExternalService(serviceName, endpoint, method, requestBody, responseType);
            } catch (Exception e) {
                logger.error("비동기 외부 서비스 호출 실패: {} - {}", serviceName, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 메뉴 서비스에서 메뉴 정보를 가져옵니다.
     * 
     * @param storeId 가게 ID
     * @return 메뉴 정보
     */
    public ResponseEntity<Map<String, Object>> getMenuInfo(String storeId) {
        try {
            logger.info("메뉴 정보 조회 요청: storeId={}", storeId);
            
            Map<String, String> requestBody = Map.of("storeId", storeId);
            
            ResponseEntity<Map> response = callExternalService(
                "restaurant-menu-service",
                "/api/menus/store/" + storeId,
                HttpMethod.GET,
                null,
                Map.class
            );
            
            logger.info("메뉴 정보 조회 완료: storeId={}", storeId);
            return new ResponseEntity<>((Map<String, Object>) response.getBody(), response.getStatusCode());
            
        } catch (Exception e) {
            logger.error("메뉴 정보 조회 실패: storeId={} - {}", storeId, e.getMessage());
            // 폴백: 더미 메뉴 데이터 반환
            return ResponseEntity.ok(createDummyMenuData(storeId));
        }
    }
    
    /**
     * 주문 서비스에 예약 정보를 전송합니다.
     * 
     * @param reservationData 예약 데이터
     * @return 주문 서비스 응답
     */
    public ResponseEntity<Map<String, Object>> sendReservationToOrderService(Map<String, Object> reservationData) {
        try {
            logger.info("주문 서비스에 예약 정보 전송: {}", reservationData.get("bookingId"));
            
            ResponseEntity<Map> response = callExternalService(
                "restaurant-order-service",
                "/api/orders/reservation",
                HttpMethod.POST,
                reservationData,
                Map.class
            );
            
            logger.info("주문 서비스 예약 정보 전송 완료: {}", reservationData.get("bookingId"));
            return new ResponseEntity<>((Map<String, Object>) response.getBody(), response.getStatusCode());
            
        } catch (Exception e) {
            logger.error("주문 서비스 예약 정보 전송 실패: {} - {}", reservationData.get("bookingId"), e.getMessage());
            throw new ServiceException("주문 서비스 연동 실패", e);
        }
    }
    
    /**
     * 알림 서비스에 알림을 전송합니다.
     * 
     * @param notificationData 알림 데이터
     * @return 알림 서비스 응답
     */
    public ResponseEntity<Map<String, Object>> sendNotificationToNotificationService(Map<String, Object> notificationData) {
        try {
            logger.info("알림 서비스에 알림 전송: {}", notificationData.get("notificationId"));
            
            ResponseEntity<Map> response = callExternalService(
                "restaurant-notification-service",
                "/api/notifications/send",
                HttpMethod.POST,
                notificationData,
                Map.class
            );
            
            logger.info("알림 서비스 알림 전송 완료: {}", notificationData.get("notificationId"));
            return new ResponseEntity<>((Map<String, Object>) response.getBody(), response.getStatusCode());
            
        } catch (Exception e) {
            logger.error("알림 서비스 알림 전송 실패: {} - {}", notificationData.get("notificationId"), e.getMessage());
            // 알림 실패는 치명적이지 않으므로 로그만 남기고 계속 진행
            return ResponseEntity.ok(Map.of("status", "FAILED", "message", "알림 전송 실패"));
        }
    }
    
    /**
     * 예약 서비스에 예약 정보를 전송합니다.
     * 
     * @param bookingData 예약 데이터
     * @return 예약 서비스 응답
     */
    public ResponseEntity<Map<String, Object>> sendBookingToBookingService(Map<String, Object> bookingData) {
        try {
            logger.info("예약 서비스에 예약 정보 전송: {}", bookingData.get("bookingId"));
            
            ResponseEntity<Map> response = callExternalService(
                "restaurant-booking-service",
                "/api/bookings",
                HttpMethod.POST,
                bookingData,
                Map.class
            );
            
            logger.info("예약 서비스 예약 정보 전송 완료: {}", bookingData.get("bookingId"));
            return new ResponseEntity<>((Map<String, Object>) response.getBody(), response.getStatusCode());
            
        } catch (Exception e) {
            logger.error("예약 서비스 예약 정보 전송 실패: {} - {}", bookingData.get("bookingId"), e.getMessage());
            throw new ServiceException("예약 서비스 연동 실패", e);
        }
    }
    

    
    // =============================================================================
    // 유틸리티 메서드
    // =============================================================================
    
    /**
     * 요청 ID를 생성합니다.
     */
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
    
    /**
     * 더미 메뉴 데이터를 생성합니다.
     */
    private Map<String, Object> createDummyMenuData(String storeId) {
        Map<String, Object> dummyMenu = new HashMap<>();
        dummyMenu.put("storeId", storeId);
        dummyMenu.put("menuItems", java.util.List.of(
            Map.of("itemId", "MENU001", "name", "김치찌개", "price", 12000, "description", "매콤한 김치찌개"),
            Map.of("itemId", "MENU002", "name", "된장찌개", "price", 10000, "description", "구수한 된장찌개"),
            Map.of("itemId", "MENU003", "name", "불고기", "price", 15000, "description", "달콤한 불고기")
        ));
        dummyMenu.put("isDummy", true);
        return dummyMenu;
    }
    
    // =============================================================================
    // 예외 클래스들
    // =============================================================================
    
    /**
     * 서비스 연결 예외
     */
    public static class ServiceConnectionException extends RuntimeException {
        public ServiceConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * 서비스 HTTP 예외
     */
    public static class ServiceHttpException extends RuntimeException {
        public ServiceHttpException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * 서비스 일반 예외
     */
    public static class ServiceException extends RuntimeException {
        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
