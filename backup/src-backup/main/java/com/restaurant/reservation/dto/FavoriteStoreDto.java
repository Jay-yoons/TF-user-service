package com.restaurant.reservation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 즐겨찾기 가게 정보를 담는 DTO 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStoreDto {
    private Long favStoreId;
    private String storeId;
    private String storeName;
    private String userId;
}
