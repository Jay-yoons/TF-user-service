package com.restaurant.reservation.exception;

/**
 * 다른 서비스와의 연결 실패 시 발생하는 예외
 */
public class ServiceConnectionException extends RuntimeException {
    
    public ServiceConnectionException(String message) {
        super(message);
    }
    
    public ServiceConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
