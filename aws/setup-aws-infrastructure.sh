#!/bin/bash

# 🏗️ Team-FOG AWS MSA 인프라 설정 스크립트
# ECS, ECR, VPC, RDS, Cognito 등 필수 서비스 생성

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 환경변수 설정
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
PROJECT_NAME="team-fog"
ENVIRONMENT="prod"

# VPC 설정
VPC_CIDR="10.0.0.0/16"
PUBLIC_SUBNET_1_CIDR="10.0.1.0/24"
PUBLIC_SUBNET_2_CIDR="10.0.2.0/24"
PRIVATE_SUBNET_1_CIDR="10.0.3.0/24"
PRIVATE_SUBNET_2_CIDR="10.0.4.0/24"

# 데이터베이스 설정
DB_INSTANCE_CLASS="db.t3.micro"
DB_NAME="teamfogdb"
DB_USERNAME="admin"

# ECS 설정
ECS_CLUSTER_NAME="${PROJECT_NAME}-cluster"
ECS_SERVICE_NAME="${PROJECT_NAME}-service"

# ECR 저장소 목록
ECR_REPOSITORIES=(
    "team-fog-user-service"
    "team-fog-reservation-service"
    "team-fog-store-service"
)

# 환경 확인
check_environment() {
    log_info "AWS 환경 확인 중..."
    
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI가 설치되지 않았습니다."
        exit 1
    fi
    
    if [ -z "$AWS_ACCOUNT_ID" ]; then
        log_error "AWS 계정 ID를 가져올 수 없습니다."
        exit 1
    fi
    
    log_success "AWS 환경 확인 완료"
    log_info "AWS 계정 ID: $AWS_ACCOUNT_ID"
    log_info "AWS 리전: $AWS_REGION"
}

# VPC 생성
create_vpc() {
    log_info "VPC 생성 중..."
    
    # VPC 생성
    VPC_ID=$(aws ec2 create-vpc \
        --cidr-block $VPC_CIDR \
        --tag-specifications ResourceType=vpc,Tags=[{Key=Name,Value=${PROJECT_NAME}-vpc}] \
        --query 'Vpc.VpcId' --output text)
    
    # VPC DNS 설정 활성화
    aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames
    aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-support
    
    log_success "VPC 생성 완료: $VPC_ID"
    
    # 인터넷 게이트웨이 생성 및 연결
    IGW_ID=$(aws ec2 create-internet-gateway \
        --tag-specifications ResourceType=internet-gateway,Tags=[{Key=Name,Value=${PROJECT_NAME}-igw}] \
        --query 'InternetGateway.InternetGatewayId' --output text)
    
    aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID
    
    # 퍼블릭 서브넷 생성
    PUBLIC_SUBNET_1_ID=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block $PUBLIC_SUBNET_1_CIDR \
        --availability-zone ${AWS_REGION}a \
        --tag-specifications ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-subnet-1}] \
        --query 'Subnet.SubnetId' --output text)
    
    PUBLIC_SUBNET_2_ID=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block $PUBLIC_SUBNET_2_CIDR \
        --availability-zone ${AWS_REGION}c \
        --tag-specifications ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-subnet-2}] \
        --query 'Subnet.SubnetId' --output text)
    
    # 프라이빗 서브넷 생성
    PRIVATE_SUBNET_1_ID=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block $PRIVATE_SUBNET_1_CIDR \
        --availability-zone ${AWS_REGION}a \
        --tag-specifications ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-private-subnet-1}] \
        --query 'Subnet.SubnetId' --output text)
    
    PRIVATE_SUBNET_2_ID=$(aws ec2 create-subnet \
        --vpc-id $VPC_ID \
        --cidr-block $PRIVATE_SUBNET_2_CIDR \
        --availability-zone ${AWS_REGION}c \
        --tag-specifications ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-private-subnet-2}] \
        --query 'Subnet.SubnetId' --output text)
    
    # 라우팅 테이블 생성
    ROUTE_TABLE_ID=$(aws ec2 create-route-table \
        --vpc-id $VPC_ID \
        --tag-specifications ResourceType=route-table,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-rt}] \
        --query 'RouteTable.RouteTableId' --output text)
    
    # 인터넷 게이트웨이 라우트 추가
    aws ec2 create-route --route-table-id $ROUTE_TABLE_ID --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW_ID
    
    # 퍼블릭 서브넷을 라우팅 테이블에 연결
    aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_1_ID --route-table-id $ROUTE_TABLE_ID
    aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_2_ID --route-table-id $ROUTE_TABLE_ID
    
    # 보안 그룹 생성
    SECURITY_GROUP_ID=$(aws ec2 create-security-group \
        --group-name ${PROJECT_NAME}-sg \
        --description "Security group for ${PROJECT_NAME}" \
        --vpc-id $VPC_ID \
        --query 'GroupId' --output text)
    
    # 보안 그룹 규칙 추가
    aws ec2 authorize-security-group-ingress --group-id $SECURITY_GROUP_ID --protocol tcp --port 80 --cidr 0.0.0.0/0
    aws ec2 authorize-security-group-ingress --group-id $SECURITY_GROUP_ID --protocol tcp --port 443 --cidr 0.0.0.0/0
    aws ec2 authorize-security-group-ingress --group-id $SECURITY_GROUP_ID --protocol tcp --port 8080-8090 --cidr 0.0.0.0/0
    
    log_success "네트워크 인프라 생성 완료"
    
    # 환경변수 저장
    echo "VPC_ID=$VPC_ID" > .env.aws
    echo "SUBNET_IDS=$PUBLIC_SUBNET_1_ID,$PUBLIC_SUBNET_2_ID,$PRIVATE_SUBNET_1_ID,$PRIVATE_SUBNET_2_ID" >> .env.aws
    echo "SECURITY_GROUP_ID=$SECURITY_GROUP_ID" >> .env.aws
}

# ECR 저장소 생성
create_ecr_repositories() {
    log_info "ECR 저장소 생성 중..."
    
    for repo in "${ECR_REPOSITORIES[@]}"; do
        log_info "ECR 저장소 생성: $repo"
        aws ecr create-repository \
            --repository-name $repo \
            --image-scanning-configuration scanOnPush=true \
            --encryption-configuration encryptionType=AES256
        
        log_success "ECR 저장소 생성 완료: $repo"
    done
}

# ECS 클러스터 생성
create_ecs_cluster() {
    log_info "ECS 클러스터 생성 중..."
    
    aws ecs create-cluster \
        --cluster-name $ECS_CLUSTER_NAME \
        --capacity-providers FARGATE \
        --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1 \
        --settings name=containerInsights,value=enabled
    
    log_success "ECS 클러스터 생성 완료: $ECS_CLUSTER_NAME"
}

# EC2 Oracle DB 연결 설정
setup_ec2_oracle_db() {
    log_info "EC2 Oracle DB 연결 설정 중..."
    
    # DB 담당자에게 필요한 정보 요청
    log_warning "DB 담당자에게 다음 정보를 요청하세요:"
    log_info "1. EC2 인스턴스의 Private IP 주소"
    log_info "2. Oracle DB 포트 (기본: 1521)"
    log_info "3. Oracle SID 또는 Service Name"
    log_info "4. DB 사용자명"
    log_info "5. DB 비밀번호"
    log_info "6. EC2 인스턴스의 보안 그룹 ID"
    
    # 보안 그룹에 Oracle DB 포트 추가
    aws ec2 authorize-security-group-ingress \
        --group-id $SECURITY_GROUP_ID \
        --protocol tcp \
        --port 1521 \
        --source-group $SECURITY_GROUP_ID \
        --description "Oracle DB access from ECS tasks"
    
    # Oracle DB 연결을 위한 환경변수 템플릿 생성
    cat > aws/oracle-db-config-template.env << EOF
# Oracle DB 연결 설정 (DB 담당자가 제공한 값으로 수정 필요)
DB_HOST=YOUR_EC2_PRIVATE_IP
DB_PORT=1521
DB_SID=YOUR_ORACLE_SID
DB_SERVICE_NAME=YOUR_SERVICE_NAME
DB_USERNAME=YOUR_DB_USERNAME
DB_PASSWORD=YOUR_DB_PASSWORD

# Oracle JDBC URL 예시
# jdbc:oracle:thin:@YOUR_EC2_PRIVATE_IP:1521:YOUR_ORACLE_SID
# 또는
# jdbc:oracle:thin:@YOUR_EC2_PRIVATE_IP:1521/YOUR_SERVICE_NAME
EOF
    
    log_success "EC2 Oracle DB 연결 설정 완료"
    log_info "설정 파일: aws/oracle-db-config-template.env"
    log_warning "DB 담당자에게 정보를 받아서 위 파일을 수정하세요!"
}

# Cognito 사용자 풀 생성
create_cognito_user_pool() {
    log_info "Cognito 사용자 풀 생성 중..."
    
    USER_POOL_ID=$(aws cognito-idp create-user-pool \
        --pool-name "${PROJECT_NAME}-user-pool" \
        --policies PasswordPolicy={MinimumLength=8,RequireUppercase=true,RequireLowercase=true,RequireNumbers=true,RequireSymbols=false} \
        --auto-verified-attributes email \
        --username-attributes email \
        --query 'UserPool.Id' --output text)
    
    # 사용자 풀 클라이언트 생성
    CLIENT_ID=$(aws cognito-idp create-user-pool-client \
        --user-pool-id $USER_POOL_ID \
        --client-name "${PROJECT_NAME}-client" \
        --no-generate-secret \
        --explicit-auth-flows ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH \
        --query 'UserPoolClient.ClientId' --output text)
    
    # 사용자 풀 도메인 생성
    aws cognito-idp create-user-pool-domain \
        --domain "${PROJECT_NAME}-auth" \
        --user-pool-id $USER_POOL_ID
    
    log_success "Cognito 사용자 풀 생성 완료"
    log_info "User Pool ID: $USER_POOL_ID"
    log_info "Client ID: $CLIENT_ID"
    
    # 환경변수에 Cognito 정보 저장
    echo "COGNITO_USER_POOL_ID=$USER_POOL_ID" >> .env.aws
    echo "COGNITO_CLIENT_ID=$CLIENT_ID" >> .env.aws
}

# Secrets Manager에 시크릿 저장
create_secrets() {
    log_info "Secrets Manager에 시크릿 생성 중..."
    
    # Cognito 시크릿
    aws secretsmanager create-secret \
        --name "${PROJECT_NAME}/cognito-config" \
        --description "Cognito configuration for ${PROJECT_NAME}" \
        --secret-string "{\"userPoolId\":\"$USER_POOL_ID\",\"clientId\":\"$CLIENT_ID\"}"
    
    log_success "Secrets Manager 시크릿 생성 완료"
    log_warning "Oracle DB 시크릿은 DB 담당자가 제공한 정보로 별도 생성 필요"
}

# IAM 역할 생성
create_iam_roles() {
    log_info "IAM 역할 생성 중..."
    
    # ECS Task Execution Role
    aws iam create-role \
        --role-name "${PROJECT_NAME}-ecs-task-execution-role" \
        --assume-role-policy-document '{
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {
                        "Service": "ecs-tasks.amazonaws.com"
                    },
                    "Action": "sts:AssumeRole"
                }
            ]
        }'
    
    # ECS Task Execution Role에 정책 연결
    aws iam attach-role-policy \
        --role-name "${PROJECT_NAME}-ecs-task-execution-role" \
        --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
    
    # Secrets Manager 접근 권한 추가
    aws iam put-role-policy \
        --role-name "${PROJECT_NAME}-ecs-task-execution-role" \
        --policy-name "SecretsManagerAccess" \
        --policy-document '{
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Action": [
                        "secretsmanager:GetSecretValue"
                    ],
                    "Resource": "*"
                }
            ]
        }'
    
    log_success "IAM 역할 생성 완료"
}

# 메인 실행 함수
main() {
    log_info "🚀 Team-FOG AWS MSA 인프라 설정 시작"
    
    check_environment
    create_vpc
    create_ecr_repositories
    create_ecs_cluster
    setup_ec2_oracle_db
    create_cognito_user_pool
    create_secrets
    create_iam_roles
    
    log_success "✅ AWS MSA 인프라 설정 완료!"
    log_info "다음 단계: 각 서비스별 Docker 이미지 빌드 및 배포"
    log_info "환경변수 파일: .env.aws"
}

# 스크립트 실행
main "$@"
