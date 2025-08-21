package com.restaurant.reservation.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Oracle Database DataSource 설정 클래스
 * 
 * AWS MSA 환경에서 Oracle DB 연결을 위한 DataSource를 구성합니다.
 * 실제 배포환경 설정
 * 
 * @author Team-FOG
 * @version 1.0
 * @since 2025-08-12
 */
// @Configuration
// @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
public class OracleDataSourceConfig {
    
    @Autowired
    private OracleConfig oracleConfig;
    
    /**
     * Oracle Database DataSource Bean 생성
     * 
     * @return Oracle DB용 DataSource
     */
    @Bean
    @Primary
    public DataSource oracleDataSource() {
        HikariConfig config = new HikariConfig();
        
        // 기본 연결 설정
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setJdbcUrl(oracleConfig.getUrl());
        config.setUsername(oracleConfig.getUsername());
        config.setPassword(oracleConfig.getPassword());
        
        // 커넥션 풀 설정
        config.setMaximumPoolSize(oracleConfig.getMaxPoolSize());
        config.setMinimumIdle(oracleConfig.getMinPoolSize());
        config.setConnectionTimeout(oracleConfig.getConnectionTimeout());
        config.setIdleTimeout(oracleConfig.getIdleTimeout());
        config.setMaxLifetime(1800000); // 30분
        
        // 연결 테스트 설정
        config.setConnectionTestQuery("SELECT 1 FROM DUAL");
        config.setValidationTimeout(5000);
        
        // SSL 설정 (필요시)
        if (oracleConfig.isSslEnabled()) {
            config.addDataSourceProperty("oracle.net.ssl_version", "1.2");
            config.addDataSourceProperty("oracle.net.ssl_server_dn_match", "true");
            
            if (oracleConfig.getSslTrustStore() != null && !oracleConfig.getSslTrustStore().isEmpty()) {
                config.addDataSourceProperty("javax.net.ssl.trustStore", oracleConfig.getSslTrustStore());
                config.addDataSourceProperty("javax.net.ssl.trustStorePassword", oracleConfig.getSslTrustStorePassword());
            }
        }
        
        // 성능 최적화 설정
        config.addDataSourceProperty("oracle.jdbc.timezoneAsRegion", "false");
        config.addDataSourceProperty("oracle.jdbc.fanEnabled", "false");
        config.addDataSourceProperty("oracle.jdbc.implicitCachingEnabled", "true");
        config.addDataSourceProperty("oracle.jdbc.defaultNChar", "false");
        config.addDataSourceProperty("oracle.jdbc.defaultNClob", "false");
        
        // 로깅 설정
        config.setPoolName("OracleHikariCP");
        
        return new HikariDataSource(config);
    }
}
