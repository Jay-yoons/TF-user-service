package com.restaurant.reservation.dto;

import com.restaurant.reservation.util.PhoneNumberUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
    private String userId;
    private String userName;
    private String phoneNumber;
    private String userLocation;
    
    /**
     * 전화번호를 한국 형식으로 반환
     * @return 한국 형식 전화번호 (예: 010-1234-5678)
     */
    public String getFormattedPhoneNumber() {
        return PhoneNumberUtil.toKoreanFormat(this.phoneNumber);
    }
}
