package com.restaurant.reservation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 웹 페이지 라우팅을 위한 컨트롤러
 * Vue.js 프론트엔드로 전환으로 인해 HTML 페이지 반환 대신 API 응답으로 변경
 */
@RestController
public class WebController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> index() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Team-FOG User Service API");
        response.put("status", "running");
        response.put("version", "2.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("=== User Service Health Check Started ===");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "user-service");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("port", "8080");
        response.put("version", "2.0");
        
        // 데이터베이스 연결 확인
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            response.put("database", isValid ? "UP" : "DOWN");
            log.info("=== Database connection: {} ===", isValid ? "UP" : "DOWN");
        } catch (SQLException e) {
            response.put("database", "DOWN");
            response.put("databaseError", e.getMessage());
            log.error("=== Database connection failed: {} ===", e.getMessage());
        }
        
        log.info("=== User Service Health Check Completed ===");
        return ResponseEntity.ok(response);
    }

    /**
     * 상세한 Health Check 엔드포인트
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("service", "User Service");
        response.put("version", "1.0.0");
        response.put("status", "UP");
        
        // 메모리 상태
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> memoryStatus = new HashMap<>();
        memoryStatus.put("total", totalMemory);
        memoryStatus.put("used", usedMemory);
        memoryStatus.put("free", freeMemory);
        memoryStatus.put("usagePercent", (double) usedMemory / totalMemory * 100);
        
        response.put("memory", memoryStatus);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health Check 로그 테스트
     */
    @GetMapping("/health/log-test")
    public ResponseEntity<String> healthLogTest() {
        return ResponseEntity.ok("Health Check Log Test Completed - " + java.time.LocalDateTime.now());
    }
} 