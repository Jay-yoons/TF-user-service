package com.restaurant.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Oracle Database 설정 클래스
 * 
 * AWS MSA 환경에서 Oracle DB 연결을 위한 설정을 관리합니다.
 * 실제 배포환경 설정
 * 
 * @author Team-FOG
 * @version 1.0
 * @since 2025-08-12
 */
// @Configuration
// @ConfigurationProperties(prefix = "oracle")
public class OracleConfig {
    
    private String host;
    private int port;
    private String serviceName;
    private String username;
    private String password;
    private String url;
    private int maxPoolSize;
    private int minPoolSize;
    private int connectionTimeout;
    private int idleTimeout;
    private boolean sslEnabled;
    private String sslTrustStore;
    private String sslTrustStorePassword;
    
    // 기본값 설정
    public OracleConfig() {
        this.port = 1521;
        this.maxPoolSize = 20;
        this.minPoolSize = 5;
        this.connectionTimeout = 30000;
        this.idleTimeout = 600000;
        this.sslEnabled = false;
    }
    
    // Getter and Setter methods
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getUrl() {
        if (url != null && !url.isEmpty()) {
            return url;
        }
        // JDBC URL 자동 생성
        return String.format("jdbc:oracle:thin:@%s:%d/%s", host, port, serviceName);
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    public int getMinPoolSize() {
        return minPoolSize;
    }
    
    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getIdleTimeout() {
        return idleTimeout;
    }
    
    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }
    
    public boolean isSslEnabled() {
        return sslEnabled;
    }
    
    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }
    
    public String getSslTrustStore() {
        return sslTrustStore;
    }
    
    public void setSslTrustStore(String sslTrustStore) {
        this.sslTrustStore = sslTrustStore;
    }
    
    public String getSslTrustStorePassword() {
        return sslTrustStorePassword;
    }
    
    public void setSslTrustStorePassword(String sslTrustStorePassword) {
        this.sslTrustStorePassword = sslTrustStorePassword;
    }
    
    @Override
    public String toString() {
        return "OracleConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", serviceName='" + serviceName + '\'' +
                ", username='" + username + '\'' +
                ", url='" + getUrl() + '\'' +
                ", maxPoolSize=" + maxPoolSize +
                ", minPoolSize=" + minPoolSize +
                ", connectionTimeout=" + connectionTimeout +
                ", idleTimeout=" + idleTimeout +
                ", sslEnabled=" + sslEnabled +
                '}';
    }
}
