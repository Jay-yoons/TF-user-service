package com.restaurant.reservation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 리뷰 정보를 담는 DTO 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long reviewId;
    private String storeId;
    private String storeName;
    private String userId;
    private String userName;
    private String content;
    private Integer rating;
}
