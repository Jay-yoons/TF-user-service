package com.restaurant.reservation.exception;

/**
 * 다른 서비스와의 HTTP 통신 실패 시 발생하는 예외
 */
public class ServiceHttpException extends RuntimeException {
    
    public ServiceHttpException(String message) {
        super(message);
    }
    
    public ServiceHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
