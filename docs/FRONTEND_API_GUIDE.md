# 🚀 Team-FOG User Service API 가이드 (배포환경)

## 📋 개요
Team-FOG User Service의 실제 배포환경에서 사용할 수 있는 API 가이드입니다.

## 🔧 서버 정보
- **URL**: `https://your-domain.com` (실제 배포 URL)
- **상태**: AWS Cognito 인증 활성화 (프로덕션 모드)
- **데이터베이스**: Oracle Database (EC2)
- **인증**: AWS Cognito JWT 토큰 필요

## 🔐 인증 방식

### AWS Cognito 로그인 플로우
1. **로그인 URL 생성**
```http
GET https://your-domain.com/api/users/login/url
```

2. **사용자 로그인** (AWS Cognito 호스팅 UI)
3. **콜백 처리**
```http
POST https://your-domain.com/api/users/login/callback
Content-Type: application/json

{
  "code": "authorization_code_from_cognito",
  "state": "state_parameter"
}
```

4. **API 호출 시 토큰 사용**
```http
Authorization: Bearer {access_token}
```

## 📡 사용 가능한 API 엔드포인트

### 1. 공개 API (인증 불필요)

#### 1.1 헬스 체크
```http
GET https://your-domain.com/actuator/health
```

#### 1.2 전체 사용자 수 조회
```http
GET https://your-domain.com/api/users/count
```

#### 1.3 회원가입
```http
POST https://your-domain.com/api/users
Content-Type: application/json

{
  "userId": "user123",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "userLocation": "서울시 강남구"
}
```

### 2. 인증 필요 API

#### 2.1 통합 마이페이지 조회
```http
GET https://your-domain.com/api/users/me
Authorization: Bearer {access_token}
```

#### 2.2 사용자 정보 수정
```http
PUT https://your-domain.com/api/users/me
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "userName": "수정된이름",
  "phoneNumber": "010-9876-5432",
  "userLocation": "서울시 서초구"
}
```

#### 2.3 즐겨찾기 가게 목록 조회
```http
GET https://your-domain.com/api/users/me/favorites
Authorization: Bearer {access_token}
```

#### 2.4 즐겨찾기 가게 추가
```http
POST https://your-domain.com/api/users/me/favorites
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "storeId": "store001"
}
```

#### 2.5 즐겨찾기 가게 삭제
```http
DELETE https://your-domain.com/api/users/me/favorites/store001
Authorization: Bearer {access_token}
```

#### 2.6 즐겨찾기 상태 확인
```http
GET https://your-domain.com/api/users/me/favorites/store001/check
Authorization: Bearer {access_token}
```

#### 2.7 내 리뷰 목록 조회
```http
GET https://your-domain.com/api/users/me/reviews
Authorization: Bearer {access_token}
```

#### 2.8 로그아웃
```http
POST https://your-domain.com/api/users/logout
Authorization: Bearer {access_token}
```

## 🧪 테스트 시나리오

### 시나리오 1: 사용자 인증 플로우
1. 로그인 URL 생성
2. AWS Cognito를 통한 로그인
3. 액세스 토큰 획득
4. 보호된 API 호출

### 시나리오 2: 사용자 관리
1. 회원가입
2. 로그인 및 토큰 획득
3. 사용자 정보 조회
4. 사용자 정보 수정

### 시나리오 3: 즐겨찾기 기능
1. 로그인 및 토큰 획득
2. 즐겨찾기 가게 추가
3. 즐겨찾기 목록 조회
4. 즐겨찾기 상태 확인
5. 즐겨찾기 가게 삭제

## ⚠️ 주의사항

1. **인증 필수**: 대부분의 API는 AWS Cognito JWT 토큰이 필요합니다.
2. **토큰 만료**: 액세스 토큰은 1시간 후 만료되므로 갱신이 필요합니다.
3. **CORS 설정**: 허용된 도메인에서만 API 호출이 가능합니다.
4. **데이터베이스**: Oracle Database를 사용하므로 연결 설정이 필요합니다.

## 🔄 환경변수 설정

배포 시 다음 환경변수를 설정해야 합니다:

```bash
# Oracle Database
ORACLE_HOST=your-oracle-host.com
ORACLE_PORT=1521
ORACLE_SERVICE_NAME=your-service-name
ORACLE_USERNAME=your-username
ORACLE_PASSWORD=your-password

# AWS Cognito
AWS_COGNITO_USER_POOL_ID=ap-northeast-2_bdkXgjghs
AWS_COGNITO_CLIENT_ID=2gjbllg398pvoe07n4oo39nvrb
AWS_COGNITO_CLIENT_SECRET=your-client-secret

# JWT
JWT_SECRET=your-jwt-secret-key-2024

# 서버
SERVER_PORT=8082
SPRING_PROFILES_ACTIVE=prod
```

## 📞 지원

문제가 발생하면 백엔드 팀에 문의하세요.

## 🔗 관련 문서

- [AWS Cognito 설정 가이드](./docs/AWS_MSA_SETUP_GUIDE.md)
- [배포 가이드](./docs/PRODUCTION_DEPLOYMENT_GUIDE.md)
- [데이터베이스 스키마](./docs/COMPLETE_DB_SCHEMA.md)
