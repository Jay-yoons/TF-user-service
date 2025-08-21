package com.restaurant.reservation.controller;

import com.restaurant.reservation.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/check-userid")
    public ResponseEntity<Boolean> checkUserIdDuplicate(@RequestParam String userId) {
        try {
            logger.info("아이디 중복 확인 요청: userId={}", userId);
            
            boolean isDuplicate = userService.isUserIdDuplicate(userId);
            logger.info("아이디 중복 확인 완료: userId={}, isDuplicate={}", userId, isDuplicate);
            return ResponseEntity.ok(isDuplicate);
        } catch (Exception e) {
            logger.error("아이디 중복 확인 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/check-phone")
    public ResponseEntity<Boolean> checkPhoneNumberDuplicate(@RequestParam String phoneNumber) {
        try {
            logger.info("전화번호 중복 확인 요청: phoneNumber={}", phoneNumber);
            
            boolean isDuplicate = userService.isPhoneNumberDuplicate(phoneNumber);
            logger.info("전화번호 중복 확인 완료: phoneNumber={}, isDuplicate={}", phoneNumber, isDuplicate);
            return ResponseEntity.ok(isDuplicate);
        } catch (Exception e) {
            logger.error("전화번호 중복 확인 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
