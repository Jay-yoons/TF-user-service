package com.restaurant.reservation.service;

import com.restaurant.reservation.entity.User;
import com.restaurant.reservation.entity.UserNameMapping;
import com.restaurant.reservation.repository.UserRepository;
import com.restaurant.reservation.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;

/**
 * 사용자 서비스 클래스
 * 
 * 이 클래스는 사용자 인증/인가 및 사용자 정보 관리 비즈니스 로직을 처리합니다.
 * - 회원가입 및 로그인 처리
 * - 사용자 정보 관리
 * - 중복 확인 및 유효성 검사
 * 
 * MSA 원칙에 따라 사용자 인증/인가 중심으로 단순화되었습니다.
 * 
 * @author FOG Team
 * @version 2.0
 * @since 2024-01-15
 */
@Service
@Transactional // 모든 메서드에 트랜잭션 적용
public class UserService {
    
    // 로깅을 위한 Logger 인스턴스
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    // 사용자 데이터 접근을 위한 Repository
    private final UserRepository userRepository;
    

    /**
     * 생성자 - 의존성 주입
     * @param userRepository 사용자 데이터 접근 객체
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public String getUserName(String userId) {
        logger.info("username 서비스 진입");
        UserNameMapping userName = userRepository.getUserNameByUserId(userId);
        logger.info("username={}", userName.getUserName());
        return userName.getUserName();
    }


    /**
     * 회원가입
     */
    public User signup(String userId, String userName, String phoneNumber, String userLocation) {
        logger.info("회원가입 요청: userId={}, userName={}, phoneNumber={}", userId, userName, phoneNumber);
        
        // 전화번호 정규화
        String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);
        logger.info("전화번호 정규화: {} -> {}", phoneNumber, normalizedPhoneNumber);
        
        // 아이디 중복 확인
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        
        // 전화번호 중복 확인 (정규화된 번호로 확인)
        if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
            throw new RuntimeException("이미 등록된 전화번호입니다.");
        }
        
        // 사용자 생성
        User user = new User();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setUserLocation(userLocation);
        
        User savedUser = userRepository.save(user);
        logger.info("회원가입 완료: userId={}", savedUser.getUserId());
        
        return savedUser;
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
            // 전화번호 정규화
            String normalizedPhoneNumber = PhoneNumberUtil.normalizePhoneNumber(newPhoneNumber);
            logger.info("전화번호 정규화: {} -> {}", newPhoneNumber, normalizedPhoneNumber);
            
            // 전화번호 중복 확인 (자신의 전화번호는 제외, 정규화된 번호로 확인)
            if (!normalizedPhoneNumber.equals(user.getPhoneNumber()) && 
                userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
                throw new RuntimeException("이미 등록된 전화번호입니다.");
            }
            user.setPhoneNumber(normalizedPhoneNumber);
        }
        
        if (updateRequest.containsKey("userLocation")) {
            user.setUserLocation(updateRequest.get("userLocation"));
        }
        
        User updatedUser = userRepository.save(user);
        logger.info("사용자 정보 수정 완료: userId={}", updatedUser.getUserId());
        
        return updatedUser;
    }


    /**
     * 통합 마이페이지 정보 조회
     * 사용자 정보만 제공 (MSA 원칙에 따라 단순화)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMyPage(String userId) {
        logger.info("통합 마이페이지 정보 조회: userId={}", userId);
        
        Map<String, Object> myPage = new HashMap<>();
        
        try {
            // 사용자 기본 정보만 제공
            User user = userRepository.findById(userId).orElse(null);
            Map<String, Object> userInfo = new HashMap<>();
            if (user != null) {
                userInfo.put("userId", user.getUserId());
                userInfo.put("userName", user.getUserName() != null ? user.getUserName() : "정보 없음");
                userInfo.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "정보 없음");
                userInfo.put("userLocation", user.getUserLocation() != null ? user.getUserLocation() : "정보 없음");
            } else {
                // 사용자가 없을 때 기본값 설정
                userInfo.put("userId", userId);
                userInfo.put("userName", "정보 없음");
                userInfo.put("phoneNumber", "정보 없음");
                userInfo.put("userLocation", "정보 없음");
            }
            myPage.put("userInfo", userInfo);
            
            logger.info("통합 마이페이지 정보 조회 완료: userId={}", userId);
            return myPage;
            
        } catch (Exception e) {
            logger.error("통합 마이페이지 정보 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            // 오류 발생 시 기본 정보만 반환
            return getMyPageFallback(userId);
        }
    }


    /**
     * 통합 마이페이지 fallback (기본 정보만 반환)
     */
    private Map<String, Object> getMyPageFallback(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        Map<String, Object> myPage = new HashMap<>();
        
        // 사용자 기본 정보만 반환
        Map<String, Object> userInfo = new HashMap<>();
        if (user != null) {
            userInfo.put("userId", user.getUserId());
            userInfo.put("userName", user.getUserName() != null ? user.getUserName() : "정보 없음");
            userInfo.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "정보 없음");
            userInfo.put("userLocation", user.getUserLocation() != null ? user.getUserLocation() : "정보 없음");
        } else {
            // 사용자가 없을 때 기본값 설정
            userInfo.put("userId", userId);
            userInfo.put("userName", "정보 없음");
            userInfo.put("phoneNumber", "정보 없음");
            userInfo.put("userLocation", "정보 없음");
        }
        myPage.put("userInfo", userInfo);
        
        return myPage;
    }
}
