package com.restaurant.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.restaurant.reservation.repository")
@EntityScan(basePackages = "com.restaurant.reservation.entity")
@EnableScheduling
public class UserServiceApplication {

    public static void main(String[] args) {
        // 한국시간대 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.setProperty("user.timezone", "Asia/Seoul");
        
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // 애플리케이션 시작 시 한국시간대 확인
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println("현재 시간대: " + TimeZone.getDefault().getID());
        System.out.println("현재 시간: " + java.time.LocalDateTime.now());
    }
}
