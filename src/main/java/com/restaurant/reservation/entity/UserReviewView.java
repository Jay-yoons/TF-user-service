package com.restaurant.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User Service에서 사용하는 Review 뷰 엔티티
 * Store Service의 Review 데이터를 조회 전용으로 사용
 */
@Entity
@Table(name = "USER_REVIEW_VIEW")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReviewView {
    
    @Id
    @Column(name = "REVIEW_ID")
    private Long reviewId;

    @Column(name = "STORE_ID", length = 20)
    private String storeId;

    @Column(name = "STORE_NAME", length = 50)
    private String storeName;

    @Column(name = "USER_ID", length = 50)
    private String userId;

    @Column(name = "COMENT", length = 50)
    private String comment;

    @Column(name = "SCORE")
    private Integer score;

    @Column(name = "STORE_LOCATION", length = 50)
    private String storeLocation;

    @Column(name = "CATEGORY_CODE")
    private Integer categoryCode;
}
