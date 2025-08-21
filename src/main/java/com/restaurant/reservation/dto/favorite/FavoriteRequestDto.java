package com.restaurant.reservation.dto.favorite;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRequestDto {
    private String userId;
    private String storeId;
}
