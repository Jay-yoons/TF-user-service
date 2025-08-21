package com.restaurant.reservation.dto.favorite;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDto {
    private Long favStoreId;
    private String userId;
    private String storeId;
    private String storeName;
    private String storeLocation;
    private String categoryCode;
    private Double averageRating;
    private Boolean isOpen;
}
