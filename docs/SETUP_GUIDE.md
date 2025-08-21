# 🛠️ 개발 환경 설정 가이드

FOG User Service의 개발 환경을 설정하는 방법을 안내합니다.

## 📋 필수 요구사항

- **Java**: 17 이상
- **Gradle**: 8.x 이상
- **IDE**: IntelliJ IDEA, Eclipse, VS Code 등
- **Database**: Oracle Database (EC2)

## 🚀 1단계: 프로젝트 클론

```bash
git clone https://github.com/Jay-yoons/Team-FOG.git
cd Team-FOG
```

## 🔧 2단계: 환경 설정

### application.yml 설정

`src/main/resources/application.yml` 파일을 다음과 같이 설정합니다:

```yaml
spring:
  profiles:
    active: prod
  
  datasource:
    url: jdbc:oracle:thin:@your-oracle-host:1521:XE
    username: your_username
    password: your_password
    driver-class-name: oracle.jdbc.OracleDriver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.OracleDialect
        format_sql: false

aws:
  cognito:
    user-pool-id: ap-northeast-2_xxxxx
    client-id: xxxxxxxxxx
    client-secret: xxxxxxxxxx
    domain: https://xxxxx.auth.ap-northeast-2.amazoncognito.com
    region: ap-northeast-2
    jwks-url: https://cognito-idp.ap-northeast-2.amazonaws.com/xxxxx/.well-known/jwks.json
    token-endpoint: https://xxxxx.auth.ap-northeast-2.amazoncognito.com/oauth2/token
    authorize-endpoint: https://xxxxx.auth.ap-northeast-2.amazoncognito.com/oauth2/authorize
    logout-endpoint: https://xxxxx.auth.ap-northeast-2.amazoncognito.com/logout
    redirect-uri: http://localhost:3000/callback
    scope: openid profile email phone
    response-type: code
    grant-type: authorization_code

msa:
  service-urls:
    store-service: http://localhost:8081
    reservation-service: http://localhost:8080

server:
  port: 8082
```

## 🗄️ 3단계: 데이터베이스 설정

### Oracle Database 테이블 생성

```sql
-- USERS 테이블
CREATE TABLE USERS (
    USER_ID VARCHAR2(50) PRIMARY KEY,
    USER_NAME VARCHAR2(20) NOT NULL,
    PHONE_NUMBER VARCHAR2(20) NOT NULL UNIQUE,
    USER_LOCATION VARCHAR2(50)
);

-- 인덱스 생성
CREATE INDEX IDX_USERS_PHONE_NUMBER ON USERS(PHONE_NUMBER);

-- FAV_STORE 테이블
CREATE TABLE FAV_STORE (
    FAV_STORE_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USER_ID VARCHAR2(15) NOT NULL,
    STORE_ID2 VARCHAR2(20) NOT NULL,
    STORE_NAME VARCHAR2(100),
    CONSTRAINT UK_FAV_STORE_USER_STORE UNIQUE (USER_ID, STORE_ID2)
);

-- 인덱스 생성
CREATE INDEX IDX_FAV_STORE_USER_ID ON FAV_STORE(USER_ID);
CREATE INDEX IDX_FAV_STORE_STORE_ID ON FAV_STORE(STORE_ID2);

-- 외래키 제약조건
ALTER TABLE FAV_STORE 
ADD CONSTRAINT FK_FAV_STORE_USER_ID 
FOREIGN KEY (USER_ID) REFERENCES USERS(USER_ID);
```

## 🏗️ 4단계: 프로젝트 빌드

```bash
# Windows
gradlew.bat clean build

# Linux/Mac
./gradlew clean build
```

## 🚀 5단계: 애플리케이션 실행

```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

애플리케이션이 성공적으로 실행되면 다음 URL에서 접근할 수 있습니다:
- **애플리케이션**: http://localhost:8082
- **헬스체크**: http://localhost:8082/api/users/health

## 🧪 6단계: 테스트

### API 테스트

```bash
# 헬스체크
curl http://localhost:8082/api/users/health

# Cognito 로그인 URL 생성
curl http://localhost:8082/api/users/login/url
```

### JUnit 테스트

```bash
# Windows
gradlew.bat test

# Linux/Mac
./gradlew test
```

## 🔍 문제 해결

### 1. 포트 충돌
```bash
# Windows
netstat -ano | findstr :8082
taskkill /f /pid <PID>

# Linux/Mac
lsof -i :8082
kill -9 <PID>
```

### 2. 데이터베이스 연결 실패
- Oracle Database가 실행 중인지 확인
- 연결 정보(호스트, 포트, 사용자명, 비밀번호) 확인
- 방화벽 설정 확인

### 3. 빌드 실패
```bash
# 캐시 삭제 후 재빌드
gradlew.bat clean build --refresh-dependencies
```

### 4. 의존성 문제
```bash
# 의존성 트리 확인
gradlew.bat dependencies
```

## 📚 추가 리소스

- **[TEST_GUIDE.md](TEST_GUIDE.md)**: 상세한 테스트 가이드
- **[PRODUCTION_README.md](PRODUCTION_README.md)**: 프로덕션 배포 가이드
- **[README.md](README.md)**: 프로젝트 개요

## 🤝 지원

문제가 발생하면 다음 방법으로 지원을 받을 수 있습니다:

1. **로그 확인**: `logs/` 폴더의 로그 파일 확인
2. **팀 채널**: #user-service 슬랙 채널
3. **이슈 등록**: GitHub Issues

---

**FOG Team** | 2025 
