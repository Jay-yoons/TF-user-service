package com.restaurant.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * MSA 환경에서 다른 서비스들과의 연동을 위한 설정 클래스
 * 
 * 이 클래스는 다른 마이크로서비스들의 URL과 설정을 관리합니다.
 * 
 * 주요 기능:
 * 1. 다른 서비스들의 URL 설정
 * 2. 서비스별 타임아웃 설정
 * 3. 재시도 정책 설정
 * 4. 서킷 브레이커 설정
 * 
 * 사용 방법:
 * - application.yml에서 msa.* 설정을 통해 값을 변경할 수 있습니다.
 * - 각 서비스별로 다른 설정을 적용할 수 있습니다.
 * 
 * @author Team-FOG
 * @version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "msa")
public class MsaConfig {
    
    // =============================================================================
    // 서비스 URL 설정
    // =============================================================================
    
    /**
     * 각 서비스의 기본 URL 설정
     */
    private Map<String, String> serviceUrls = new HashMap<>();
    
    /**
     * 서비스별 타임아웃 설정 (밀리초)
     */
    private Map<String, Integer> timeouts = new HashMap<>();
    
    /**
     * 서비스별 재시도 횟수 설정
     */
    private Map<String, Integer> retryCounts = new HashMap<>();
    
    /**
     * 서킷 브레이커 설정
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    
    /**
     * 기본 타임아웃 설정 (밀리초)
     */
    private int defaultTimeout = 5000;
    
    /**
     * 기본 재시도 횟수
     */
    private int defaultRetryCount = 3;
    
    // =============================================================================
    // 내부 클래스
    // =============================================================================
    
    /**
     * 서킷 브레이커 설정 클래스
     */
    public static class CircuitBreakerConfig {
        private boolean enabled = true;
        private int failureThreshold = 5;
        private int recoveryTime = 60000; // 60초
        private int timeout = 3000; // 3초
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        
        public int getRecoveryTime() { return recoveryTime; }
        public void setRecoveryTime(int recoveryTime) { this.recoveryTime = recoveryTime; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }
    
    // =============================================================================
    // Getter/Setter 메서드
    // =============================================================================
    
    public Map<String, String> getServiceUrls() {
        return serviceUrls;
    }
    
    public void setServiceUrls(Map<String, String> serviceUrls) {
        this.serviceUrls = serviceUrls;
    }
    
    public Map<String, Integer> getTimeouts() {
        return timeouts;
    }
    
    public void setTimeouts(Map<String, Integer> timeouts) {
        this.timeouts = timeouts;
    }
    
    public Map<String, Integer> getRetryCounts() {
        return retryCounts;
    }
    
    public void setRetryCounts(Map<String, Integer> retryCounts) {
        this.retryCounts = retryCounts;
    }
    
    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }
    
    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    public int getDefaultTimeout() {
        return defaultTimeout;
    }
    
    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }
    
    public int getDefaultRetryCount() {
        return defaultRetryCount;
    }
    
    public void setDefaultRetryCount(int defaultRetryCount) {
        this.defaultRetryCount = defaultRetryCount;
    }
    
    // =============================================================================
    // 유틸리티 메서드
    // =============================================================================
    
    /**
     * 특정 서비스의 URL을 가져옵니다.
     */
    public String getServiceUrl(String serviceName) {
        return serviceUrls.getOrDefault(serviceName, "");
    }
    
    /**
     * 특정 서비스의 타임아웃을 가져옵니다.
     */
    public int getServiceTimeout(String serviceName) {
        return timeouts.getOrDefault(serviceName, defaultTimeout);
    }
    
    /**
     * 특정 서비스의 재시도 횟수를 가져옵니다.
     */
    public int getServiceRetryCount(String serviceName) {
        return retryCounts.getOrDefault(serviceName, defaultRetryCount);
    }
    
    /**
     * 서비스가 등록되어 있는지 확인합니다.
     */
    public boolean isServiceRegistered(String serviceName) {
        return serviceUrls.containsKey(serviceName);
    }
}
