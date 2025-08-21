# 🚀 Team-FOG User Service 프로덕션 배포 가이드

## 📋 **아키텍처 개요**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   AWS ALB       │    │   User Service  │
│   (Vue.js)      │◄──►│   + Cognito     │◄──►│   (Spring Boot) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │ Oracle Database │
                                              │     (EC2)       │
                                              └─────────────────┘
```

## 🏗️ **인프라 구성**

### **컨테이너 오케스트레이션**
- **AWS ECS (Elastic Container Service)**
- **AWS Fargate** (서버리스 컨테이너 실행)

### **데이터베이스**
- **Oracle Database (EC2)** - 메인 데이터베이스
- **Oracle Database (EC2)** - Standby 데이터베이스 (읽기 전용)

### **인증 시스템**
- **AWS Cognito** - 사용자 인증 및 권한 관리
- **AWS ALB + Cognito 통합 인증**

### **로드 밸런싱**
- **AWS ALB (Application Load Balancer)** - 트래픽 분산

## 🔧 **환경 변수 설정**

### **필수 환경 변수**
```bash
# Oracle Database 설정
ORACLE_HOST=your-ec2-oracle-instance-ip
ORACLE_PORT=1521
ORACLE_SERVICE_NAME=XE
ORACLE_USERNAME=your_username
ORACLE_PASSWORD=your_password

# AWS Cognito 설정
AWS_COGNITO_USER_POOL_ID=ap-northeast-2_bdkXgjghs
AWS_COGNITO_CLIENT_ID=2gjbllg398pvoe07n4oo39nvrb
AWS_COGNITO_CLIENT_SECRET=your_client_secret
AWS_COGNITO_DOMAIN=https://ap-northeast-2bdkxgjghs.auth.ap-northeast-2.amazoncognito.com
AWS_COGNITO_REGION=ap-northeast-2
AWS_COGNITO_JWKS_URL=https://cognito-idp.ap-northeast-2.amazonaws.com/ap-northeast-2_bdkXgjghs/.well-known/jwks.json

# 서비스 설정
SERVER_PORT=8082
SPRING_PROFILES_ACTIVE=prod
```

### **선택적 환경 변수**
```bash
# Standby Database 설정 (읽기 전용)
ORACLE_STANDBY_HOST=your-ec2-standby-instance-ip
ORACLE_STANDBY_PORT=1521
ORACLE_STANDBY_SERVICE_NAME=XE
ORACLE_STANDBY_USERNAME=your_standby_username
ORACLE_STANDBY_PASSWORD=your_standby_password

# 로깅 설정
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_RESTAURANT_RESERVATION=DEBUG

# JVM 설정
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

## 🗄️ **Oracle Database EC2 설정**

### **EC2 인스턴스 권장 사양**
- **인스턴스 타입**: t3.medium 이상 (2 vCPU, 4GB RAM)
- **스토리지**: 50GB 이상 (SSD)
- **보안 그룹**: 1521 포트 허용 (Oracle Listener)
- **VPC**: 프라이빗 서브넷 권장

### **Oracle Database 설치 및 설정**
```bash
# Oracle Database 설치
sudo yum update -y
sudo yum install oracle-database-preinstall-19c -y

# Oracle Database 다운로드 및 설치
# Oracle 공식 사이트에서 19c 다운로드 후 설치

# Listener 설정
sudo netca

# Database 생성
sudo dbca

# 환경 변수 설정
echo 'export ORACLE_HOME=/u01/app/oracle/product/19.0.0/dbhome_1' >> ~/.bashrc
echo 'export PATH=$ORACLE_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### **데이터베이스 스키마 생성**
```sql
-- 사용자 생성
CREATE USER team_fog_user IDENTIFIED BY your_password;

-- 권한 부여
GRANT CONNECT, RESOURCE, DBA TO team_fog_user;
GRANT CREATE SESSION TO team_fog_user;
GRANT UNLIMITED TABLESPACE TO team_fog_user;

-- 테이블스페이스 생성
CREATE TABLESPACE team_fog_data
DATAFILE '/u01/app/oracle/oradata/XE/team_fog_data.dbf'
SIZE 100M
AUTOEXTEND ON NEXT 10M MAXSIZE 1G;
```

## 🔐 **AWS ALB + Cognito 설정**

### **ALB Listener Rule 설정**
```bash
# Cognito 인증 규칙 생성
aws elbv2 create-listener-rule \
  --listener-arn arn:aws:elasticloadbalancing:ap-northeast-2:123456789012:listener/app/your-alb/1234567890123456/1234567890123456 \
  --priority 1 \
  --conditions Field=path-pattern,Values=/api/users/* \
  --actions Type=authenticate-cognito,AuthenticateCognitoConfig='{
    "UserPoolArn": "arn:aws:cognito-idp:ap-northeast-2:733995297457:userpool/ap-northeast-2_bdkXgjghs",
    "UserPoolClientId": "2gjbllg398pvoe07n4oo39nvrb",
    "UserPoolDomain": "ap-northeast-2bdkxgjghs",
    "Scope": "openid profile email phone"
  }'
```

### **ALB Target Group 설정**
```bash
# Target Group 생성
aws elbv2 create-target-group \
  --name user-service-tg \
  --protocol HTTP \
  --port 8082 \
  --vpc-id vpc-12345678 \
  --health-check-path /api/users/health \
  --health-check-interval-seconds 30 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3
```

### **보안 그룹 설정**
```bash
# ALB 보안 그룹
aws ec2 create-security-group \
  --group-name user-service-alb-sg \
  --description "Security group for User Service ALB" \
  --vpc-id vpc-12345678

# 80, 443 포트 허용
aws ec2 authorize-security-group-ingress \
  --group-id sg-12345678 \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
  --group-id sg-12345678 \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0
```

## 🐳 **Docker 빌드 및 배포**

### **1. Docker 이미지 빌드**
```bash
# 프로덕션용 이미지 빌드
docker build -t team-fog/user-service:latest .

# 이미지 태그 설정
docker tag team-fog/user-service:latest 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/team-fog/user-service:latest
```

### **2. ECR 푸시**
```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 푸시
docker push 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/team-fog/user-service:latest
```

### **3. ECS 서비스 배포**
```bash
# ECS 서비스 업데이트
aws ecs update-service \
  --cluster team-fog-cluster \
  --service user-service \
  --force-new-deployment
```

## 📊 **모니터링 및 로깅**

### **CloudWatch 로그 설정**
```bash
# 로그 그룹 생성
aws logs create-log-group --log-group-name /ecs/user-service

# 로그 스트림 생성
aws logs create-log-stream \
  --log-group-name /ecs/user-service \
  --log-stream-name user-service-$(date +%Y-%m-%d)
```

### **메트릭 모니터링**
- **CPU 사용률**: 70% 이하 유지
- **메모리 사용률**: 80% 이하 유지
- **응답 시간**: 500ms 이하
- **에러율**: 1% 이하

## 🚨 **문제 해결**

### **1. 데이터베이스 연결 실패**
```bash
# Oracle Listener 상태 확인
lsnrctl status

# 데이터베이스 연결 테스트
sqlplus team_fog_user/your_password@//localhost:1521/XE

# 네트워크 연결 확인
telnet your-ec2-oracle-instance-ip 1521

# 보안 그룹 확인
aws ec2 describe-security-groups --group-ids sg-12345678
```

### **2. ALB Cognito 인증 실패**
```bash
# Cognito User Pool 상태 확인
aws cognito-idp describe-user-pool --user-pool-id ap-northeast-2_bdkXgjghs

# ALB Listener 규칙 확인
aws elbv2 describe-listener-rules --listener-arn arn:aws:elasticloadbalancing:ap-northeast-2:123456789012:listener/app/your-alb/1234567890123456/1234567890123456

# CloudWatch 로그 확인
aws logs filter-log-events \
  --log-group-name /aws/applicationloadbalancer/your-alb \
  --start-time $(date -d '1 hour ago' +%s)000
```

### **3. 애플리케이션 시작 실패**
```bash
# ECS 태스크 로그 확인
aws logs get-log-events \
  --log-group-name /ecs/user-service \
  --log-stream-name user-service-$(date +%Y-%m-%d) \
  --start-time $(date -d '10 minutes ago' +%s)000

# 환경 변수 확인
aws ecs describe-task-definition --task-definition user-service:latest
```

### **4. 메모리 부족**
```bash
# JVM 힙 메모리 증가
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"

# ECS 태스크 메모리 증가
aws ecs register-task-definition \
  --family user-service \
  --memory 2048 \
  --cpu 1024
```

## 🔄 **백업 및 복구**

### **데이터베이스 백업**
```bash
# Oracle RMAN 백업
rman target /
BACKUP DATABASE PLUS ARCHIVELOG;
BACKUP CURRENT CONTROLFILE TO '/backup/controlfile.bak';
```

### **애플리케이션 백업**
```bash
# ECS 태스크 정의 백업
aws ecs describe-task-definition --task-definition user-service:latest > task-definition-backup.json

# 환경 변수 백업
aws ssm get-parameters --names /team-fog/user-service/* --with-decryption > env-backup.json
```

## 📈 **성능 최적화**

### **JVM 튜닝**
```bash
# G1GC 가비지 컬렉터 사용
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45"

# 힙 메모리 설정
JAVA_OPTS="$JAVA_OPTS -Xms1g -Xmx2g"
```

### **데이터베이스 튜닝**
```sql
-- 인덱스 생성
CREATE INDEX idx_users_user_id ON users(user_id);
CREATE INDEX idx_favorite_stores_user_id ON favorite_stores(user_id);

-- 통계 정보 업데이트
ANALYZE TABLE users COMPUTE STATISTICS;
ANALYZE TABLE favorite_stores COMPUTE STATISTICS;
```

## 🔒 **보안 설정**

### **네트워크 보안**
- **VPC**: 프라이빗 서브넷 사용
- **보안 그룹**: 최소 권한 원칙 적용
- **NACL**: 네트워크 액세스 제어

### **데이터 보안**
- **암호화**: 저장 데이터 암호화 (AES-256)
- **전송 암호화**: TLS 1.2 이상 사용
- **접근 제어**: IAM 역할 기반 접근 제어

---

**버전**: 2.0  
**최종 업데이트**: 2024년 1월 15일  
**작성자**: Team-FOG
