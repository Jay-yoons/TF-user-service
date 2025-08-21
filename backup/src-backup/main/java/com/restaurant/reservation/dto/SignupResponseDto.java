package com.restaurant.reservation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponseDto {
    private String userId;
    private String userName;
    private String phoneNumber;
    private String userLocation;
}
