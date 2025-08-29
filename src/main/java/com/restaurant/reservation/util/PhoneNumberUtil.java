package com.restaurant.reservation.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 전화번호 정규화 유틸리티 클래스
 * 
 * 사용자가 다양한 형식으로 입력한 전화번호를 표준 형식으로 변환합니다.
 * 
 * @author Team-FOG
 * @version 1.0
 * @since 2024-01-15
 */
public class PhoneNumberUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PhoneNumberUtil.class);
    
    /**
     * 전화번호를 국제 형식으로 정규화
     * 
     * @param phoneNumber 입력된 전화번호 (예: 01012345678, 010-1234-5678, +82 10 1234 5678)
     * @return 정규화된 전화번호 (예: +82 10 1234 5678)
     */
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }
        
        // +01, +02 등 잘못된 형식 자동 수정
        if (phoneNumber.startsWith("+0")) {
            logger.info("잘못된 전화번호 형식 자동 수정: {} -> {}", phoneNumber, phoneNumber.substring(2));
            phoneNumber = phoneNumber.substring(2);
        }
        
        // 공백과 특수문자 제거
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        
        // 이미 +82 형식인 경우 그대로 반환
        if (cleaned.startsWith("+82")) {
            return formatInternationalNumber(cleaned);
        }
        
        // 82로 시작하는 경우 + 추가
        if (cleaned.startsWith("82")) {
            return formatInternationalNumber("+" + cleaned);
        }
        
        // 0으로 시작하는 경우 +82로 변환
        if (cleaned.startsWith("0")) {
            return formatInternationalNumber("+82" + cleaned.substring(1));
        }
        
        // 10자리 또는 11자리 숫자인 경우, 0으로 시작하지 않을 때만 0 추가
        if (cleaned.length() == 10 || cleaned.length() == 11) {
            // 이미 0으로 시작하는 경우는 그대로 처리
            if (cleaned.startsWith("0")) {
                return formatInternationalNumber("+82" + cleaned.substring(1));
            }
            // 0으로 시작하지 않는 경우에만 0 추가
            cleaned = "0" + cleaned;
            return formatInternationalNumber("+82" + cleaned.substring(1));
        }
        
        // 형식에 맞지 않는 경우 원본 반환
        logger.warn("전화번호 형식이 올바르지 않습니다: {}", phoneNumber);
        return phoneNumber;
    }
    
    /**
     * 국제 형식으로 포맷팅
     * 
     * @param internationalNumber 국제 형식 전화번호 (예: +821012345678)
     * @return 포맷된 전화번호 (예: +82 10 1234 5678)
     */
    private static String formatInternationalNumber(String internationalNumber) {
        if (internationalNumber == null || internationalNumber.length() < 12) {
            return internationalNumber;
        }
        
        // +82 제거 후 나머지 부분 포맷팅
        String numberPart = internationalNumber.substring(3);
        
        if (numberPart.startsWith("10")) {
            // 휴대폰 번호: 10 xxx xxxx
            return "+82 " + numberPart.substring(0, 2) + " " + 
                   numberPart.substring(2, 6) + " " + numberPart.substring(6);
        } else if (numberPart.startsWith("2")) {
            // 서울 지역번호: 2 xxx xxxx
            return "+82 " + numberPart.substring(0, 1) + " " + 
                   numberPart.substring(1, 5) + " " + numberPart.substring(5);
        } else {
            // 기타 지역번호: xx xxx xxxx
            return "+82 " + numberPart.substring(0, 2) + " " + 
                   numberPart.substring(2, 6) + " " + numberPart.substring(6);
        }
    }
    
    /**
     * 전화번호 유효성 검사
     * 
     * @param phoneNumber 검사할 전화번호
     * @return 유효성 여부
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // +01, +02 등 잘못된 형식 검사
        if (phoneNumber.startsWith("+0")) {
            logger.warn("잘못된 전화번호 형식: {}", phoneNumber);
            return false;
        }
        
        // 공백과 특수문자 제거
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        
        // +82 형식인 경우
        if (cleaned.startsWith("+82")) {
            cleaned = cleaned.substring(3);
        }
        // 82로 시작하는 경우
        else if (cleaned.startsWith("82")) {
            cleaned = cleaned.substring(2);
        }
        // 0으로 시작하는 경우
        else if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }
        
        // 한국 전화번호 패턴 검사
        return cleaned.matches("^(10|2|3[0-9]|4[0-9]|5[0-9]|6[0-9]|7[0-9]|8[0-9]|9[0-9])\\d{7,8}$");
    }
    
    /**
     * 전화번호를 한국 형식으로 변환 (표시용)
     * 
     * @param phoneNumber 국제 형식 전화번호 (예: +82 10 1234 5678)
     * @return 한국 형식 전화번호 (예: 010-1234-5678)
     */
    public static String toKoreanFormat(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }
        
        // +82 형식이 아닌 경우 그대로 반환
        if (!phoneNumber.startsWith("+82")) {
            return phoneNumber;
        }
        
        // 공백 제거
        String cleaned = phoneNumber.replaceAll("\\s", "");
        
        // +82 제거
        String numberPart = cleaned.substring(3);
        
        if (numberPart.startsWith("10")) {
            // 휴대폰 번호: 010-xxxx-xxxx
            return "010-" + numberPart.substring(2, 6) + "-" + numberPart.substring(6);
        } else if (numberPart.startsWith("2")) {
            // 서울 지역번호: 02-xxxx-xxxx
            return "02-" + numberPart.substring(1, 5) + "-" + numberPart.substring(5);
        } else {
            // 기타 지역번호: 이미 0으로 시작하는지 확인 후 처리
            if (numberPart.startsWith("0")) {
                // 이미 0으로 시작하는 경우: 0xx-xxxx-xxxx
                return numberPart.substring(0, 3) + "-" + 
                       numberPart.substring(3, 7) + "-" + numberPart.substring(7);
            } else {
                // 0으로 시작하지 않는 경우: 0xx-xxxx-xxxx
                return "0" + numberPart.substring(0, 2) + "-" + 
                       numberPart.substring(2, 6) + "-" + numberPart.substring(6);
            }
        }
    }
}
