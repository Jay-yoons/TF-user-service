package com.restaurant.reservation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 웹 페이지 라우팅을 위한 컨트롤러
 * Vue.js 프론트엔드로 전환으로 인해 HTML 페이지 반환 대신 API 응답으로 변경
 */
@RestController
public class WebController {

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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "user-service");
        return ResponseEntity.ok(response);
    }
} 