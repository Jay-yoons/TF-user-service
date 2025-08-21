# 🧪 Team-FOG User Service 테스트 가이드

## 📋 **테스트 환경 설정**

### 1. **프로젝트 빌드 및 실행**

#### **Windows 환경**
```bash
# 프로젝트 빌드
gradlew.bat clean build

# 애플리케이션 실행
gradlew.bat bootRun
```

#### **Linux/Mac 환경**
```bash
# 프로젝트 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew bootRun
```

### 2. **애플리케이션 접속**
- **URL**: http://localhost:8082
- **H2 Console**: http://localhost:8082/h2-console
- **API Base URL**: http://localhost:8082/api

## 🔧 **테스트 시나리오**

### **1. 기본 헬스체크**
```bash
# 서비스 상태 확인
curl http://localhost:8082/api/users/health

# 예상 응답
{
  "service": "user-service",
  "status": "UP",
  "timestamp": 1234567890
}
```

### **2. 사용자 수 조회**
```bash
# 전체 사용자 수 조회
curl http://localhost:8082/api/users/count

# 예상 응답
{
  "count": 3
}
```

### **3. 회원가입 테스트**
```bash
# 회원가입 요청
curl -X POST http://localhost:8082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "testuser001",
    "userName": "테스트사용자",
    "phoneNumber": "010-1234-5678",
    "userLocation": "서울시 강남구",
    "password": "password123"
  }'

# 예상 응답
{
  "userId": "testuser001",
  "userName": "테스트사용자",
  "phoneNumber": "010-1234-5678",
  "userLocation": "서울시 강남구",
  "createdAt": "2024-01-15T10:30:00"
}
```

### **4. Cognito 로그인 URL 생성**
```bash
# Cognito 로그인 URL 생성
curl http://localhost:8082/api/users/login/url

# 예상 응답
{
  "url": "https://ap-northeast-2bdkxgjghs.auth.ap-northeast-2.amazoncognito.com/oauth2/authorize?response_type=code&client_id=2gjbllg398pvoe07n4oo39nvrb&redirect_uri=http://localhost:3000/callback&scope=openid+profile+email+phone&state=uuid-here",
  "state": "uuid-here"
}
```

### **5. H2 데이터베이스 확인**
```bash
# H2 Console 접속
# URL: http://localhost:8082/h2-console
# JDBC URL: jdbc:h2:mem:userdb
# Username: sa
# Password: (비어있음)

# 테이블 확인
SELECT * FROM USERS;
SELECT * FROM FAVORITE_STORES;
```

## 🔐 **인증 테스트**

### **1. JWT 토큰 테스트 (개발용)**
```bash
# Cognito 토큰으로 API 호출
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8082/api/users/me
```

### **2. ALB Cognito 인증 테스트**
```bash
# ALB 헤더로 API 호출
curl -H "X-Amzn-Oidc-Identity: testuser001" \
  http://localhost:8082/api/users/me
```

## 📊 **API 테스트 도구**

### **1. Postman Collection**
```json
{
  "info": {
    "name": "Team-FOG User Service API",
    "description": "User Service API 테스트"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "http://localhost:8082/api/users/health"
      }
    },
    {
      "name": "User Count",
      "request": {
        "method": "GET",
        "url": "http://localhost:8082/api/users/count"
      }
    },
    {
      "name": "Signup",
      "request": {
        "method": "POST",
        "url": "http://localhost:8082/api/users",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"userId\": \"testuser001\",\n  \"userName\": \"테스트사용자\",\n  \"phoneNumber\": \"010-1234-5678\",\n  \"userLocation\": \"서울시 강남구\",\n  \"password\": \"password123\"\n}"
        }
      }
    }
  ]
}
```

### **2. cURL 스크립트**
```bash
#!/bin/bash

# 기본 헬스체크
echo "=== Health Check ==="
curl -s http://localhost:8082/api/users/health | jq .

# 사용자 수 조회
echo -e "\n=== User Count ==="
curl -s http://localhost:8082/api/users/count | jq .

# 회원가입
echo -e "\n=== Signup ==="
curl -s -X POST http://localhost:8082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "testuser001",
    "userName": "테스트사용자",
    "phoneNumber": "010-1234-5678",
    "userLocation": "서울시 강남구",
    "password": "password123"
  }' | jq .
```

## 🚨 **문제 해결**

### **1. 포트 충돌**
```bash
# 8082 포트 사용 중인 프로세스 확인
netstat -ano | findstr :8082

# 프로세스 종료
taskkill /PID <process_id> /F
```

### **2. 데이터베이스 연결 실패**
```bash
# H2 Console 접속 확인
# URL: http://localhost:8082/h2-console
# JDBC URL: jdbc:h2:mem:userdb
```

### **3. Cognito 연결 실패**
```bash
# Cognito 설정 확인
# application.yml의 aws.cognito 설정 확인
# User Pool ID, Client ID, Domain 등 확인
```

### **4. 로그 확인**
```bash
# 애플리케이션 로그 확인
tail -f logs/user-service.log

# 콘솔 로그 확인
# 애플리케이션 실행 시 콘솔 출력 확인
```

## 📝 **테스트 체크리스트**

- [ ] 프로젝트 빌드 성공
- [ ] 애플리케이션 실행 성공
- [ ] 헬스체크 API 응답 확인
- [ ] H2 Console 접속 확인
- [ ] 회원가입 API 테스트
- [ ] 사용자 수 조회 API 테스트
- [ ] Cognito 로그인 URL 생성 테스트
- [ ] 데이터베이스 테이블 확인
- [ ] 로그 출력 확인

## 🎯 **성공 기준**

1. **애플리케이션이 정상적으로 시작됨**
2. **모든 API가 정상 응답**
3. **데이터베이스 연결 성공**
4. **Cognito 설정 로드 성공**
5. **로그에 오류 없음**

---

**버전**: 1.0  
**최종 업데이트**: 2024년 1월 15일
