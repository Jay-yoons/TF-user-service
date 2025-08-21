package com.restaurant.reservation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 사용자 즐겨찾기 가게 엔티티 클래스
 * 
 * 이 클래스는 사용자가 즐겨찾기한 가게 정보를 데이터베이스에 저장하기 위한 JPA 엔티티입니다.
 * FOG 팀의 데이터 사전에 따라 설계되었습니다.
 * 
 * 주요 필드:
 * - favStoreId: 즐겨찾기 고유 ID (기본키)
 * - userId: 사용자 ID (외래키)
 * - storeId: 가게 ID (외래키)
 * - storeName: 가게 이름
 * 
 * 제약조건:
 * - (userId, storeId) 복합 유니크 제약조건
 * 
 * @author FOG Team
 * @version 2.0
 * @since 2025-08-18
 */
@Entity
@Table(name = "FAV_STORE", uniqueConstraints = {
    @UniqueConstraint(name = "fav_store_un", columnNames = {"USER_ID", "STORE_ID2"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStore {
    
    /**
     * 즐겨찾기 고유 ID (기본키)
     * 자동 증가
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fav_store_id")
    private Long favStoreId;
    
    /**
     * 사용자 ID (외래키)
     * 최대 15자까지 저장 가능
     */
    @Column(name = "USER_ID", nullable = false, length = 15)
    private String userId;
    
    /**
     * 가게 ID (외래키)
     * 최대 20자까지 저장 가능
     */
    @Column(name = "STORE_ID2", nullable = false, length = 20)
    private String storeId;
    
    /**
     * 가게 이름 (DB 담당자가 추가한 컬럼)
     * 최대 100자까지 저장 가능
     */
    @Column(name = "STORE_NAME", length = 100)
    private String storeName;
    
    // Custom constructors for partial fields
    public FavoriteStore(String userId, String storeId) {
        this.userId = userId;
        this.storeId = storeId;
    }
    
    public FavoriteStore(String userId, String storeId, String storeName) {
        this.userId = userId;
        this.storeId = storeId;
        this.storeName = storeName;
    }
}
