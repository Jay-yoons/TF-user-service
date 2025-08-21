#!/bin/bash

# 🚀 Team-FOG User Service 배포 스크립트
# Docker 빌드 및 AWS ECS 배포 자동화

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
ECR_REPOSITORY="team-fog-user-service"
SERVICE_NAME="team-fog-user-service"
CLUSTER_NAME="team-fog-cluster"
TASK_DEFINITION="team-fog-user-service-task"

# 환경 확인
check_environment() {
    log_info "환경 설정 확인 중..."
    
    # Docker 확인
    if ! command -v docker &> /dev/null; then
        log_error "Docker가 설치되지 않았습니다."
        exit 1
    fi
    
    # AWS CLI 확인
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI가 설치되지 않았습니다."
        exit 1
    fi
    
    # AWS 계정 확인
    if [ -z "$AWS_ACCOUNT_ID" ]; then
        log_error "AWS 계정 ID를 가져올 수 없습니다. AWS CLI 설정을 확인하세요."
        exit 1
    fi
    
    log_success "환경 설정 확인 완료"
    log_info "AWS 계정 ID: $AWS_ACCOUNT_ID"
    log_info "AWS 리전: $AWS_REGION"
}

# Gradle 빌드
build_application() {
    log_info "Gradle 빌드 시작..."
    
    # Gradle 캐시 정리
    ./gradlew clean
    
    # 애플리케이션 빌드 (테스트 제외)
    ./gradlew build -x test --no-daemon --stacktrace
    
    if [ $? -eq 0 ]; then
        log_success "Gradle 빌드 완료"
    else
        log_error "Gradle 빌드 실패"
        exit 1
    fi
}

# Docker 이미지 빌드
build_docker_image() {
    log_info "Docker 이미지 빌드 시작..."
    
    # Docker 이미지 빌드 (재시도 로직 포함)
    local retry_count=0
    local max_retries=3
    
    while [ $retry_count -lt $max_retries ]; do
        if docker build -t $ECR_REPOSITORY:latest .; then
            log_success "Docker 이미지 빌드 완료"
            return 0
        else
            retry_count=$((retry_count + 1))
            log_warning "Docker 빌드 실패 (시도 $retry_count/$max_retries)"
            
            if [ $retry_count -lt $max_retries ]; then
                log_info "10초 후 재시도..."
                sleep 10
            fi
        fi
    done
    
    log_error "Docker 이미지 빌드 실패 (최대 시도 횟수 초과)"
    exit 1
}

# ECR 로그인
login_to_ecr() {
    log_info "AWS ECR 로그인 중..."
    
    aws ecr get-login-password --region $AWS_REGION | \
    docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
    
    if [ $? -eq 0 ]; then
        log_success "ECR 로그인 완료"
    else
        log_error "ECR 로그인 실패"
        exit 1
    fi
}

# Docker 이미지 태그
tag_docker_image() {
    log_info "Docker 이미지 태그 설정 중..."
    
    local ecr_url="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
    
    docker tag $ECR_REPOSITORY:latest $ecr_url:latest
    
    if [ $? -eq 0 ]; then
        log_success "Docker 이미지 태그 설정 완료"
        log_info "ECR URL: $ecr_url"
    else
        log_error "Docker 이미지 태그 설정 실패"
        exit 1
    fi
}

# ECR 푸시
push_to_ecr() {
    log_info "ECR에 이미지 푸시 중..."
    
    local ecr_url="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
    
    # 푸시 (재시도 로직 포함)
    local retry_count=0
    local max_retries=3
    
    while [ $retry_count -lt $max_retries ]; do
        if docker push $ecr_url:latest; then
            log_success "ECR 푸시 완료"
            return 0
        else
            retry_count=$((retry_count + 1))
            log_warning "ECR 푸시 실패 (시도 $retry_count/$max_retries)"
            
            if [ $retry_count -lt $max_retries ]; then
                log_info "30초 후 재시도..."
                sleep 30
            fi
        fi
    done
    
    log_error "ECR 푸시 실패 (최대 시도 횟수 초과)"
    exit 1
}

# ECS Task Definition 업데이트
update_task_definition() {
    log_info "ECS Task Definition 업데이트 중..."
    
    # Task Definition 등록
    aws ecs register-task-definition \
        --cli-input-json file://aws/ecs-task-definition.json \
        --region $AWS_REGION
    
    if [ $? -eq 0 ]; then
        log_success "Task Definition 업데이트 완료"
    else
        log_error "Task Definition 업데이트 실패"
        exit 1
    fi
}

# ECS 서비스 업데이트
update_ecs_service() {
    log_info "ECS 서비스 업데이트 중..."
    
    # 서비스 업데이트
    aws ecs update-service \
        --cluster $CLUSTER_NAME \
        --service $SERVICE_NAME \
        --force-new-deployment \
        --region $AWS_REGION
    
    if [ $? -eq 0 ]; then
        log_success "ECS 서비스 업데이트 완료"
    else
        log_error "ECS 서비스 업데이트 실패"
        exit 1
    fi
}

# 배포 상태 확인
check_deployment_status() {
    log_info "배포 상태 확인 중..."
    
    # 서비스 상태 확인
    local status=$(aws ecs describe-services \
        --cluster $CLUSTER_NAME \
        --services $SERVICE_NAME \
        --region $AWS_REGION \
        --query 'services[0].status' \
        --output text)
    
    log_info "서비스 상태: $status"
    
    # 배포 상태 확인
    local deployment_status=$(aws ecs describe-services \
        --cluster $CLUSTER_NAME \
        --services $SERVICE_NAME \
        --region $AWS_REGION \
        --query 'services[0].deployments[0].status' \
        --output text)
    
    log_info "배포 상태: $deployment_status"
    
    if [ "$deployment_status" = "PRIMARY" ]; then
        log_success "배포가 성공적으로 완료되었습니다!"
    else
        log_warning "배포가 진행 중입니다. 잠시 후 다시 확인하세요."
    fi
}

# 정리
cleanup() {
    log_info "임시 파일 정리 중..."
    
    # 로컬 Docker 이미지 삭제
    docker rmi $ECR_REPOSITORY:latest 2>/dev/null || true
    
    log_success "정리 완료"
}

# 메인 함수
main() {
    log_info "Team-FOG User Service 배포 시작"
    echo ""
    
    check_environment
    build_application
    build_docker_image
    login_to_ecr
    tag_docker_image
    push_to_ecr
    update_task_definition
    update_ecs_service
    check_deployment_status
    cleanup
    
    echo ""
    log_success "=== 배포 완료 ==="
    log_info "서비스 URL: http://[ALB_DNS_NAME]/api/users"
    log_info "헬스체크: http://[ALB_DNS_NAME]/api/users/actuator/health"
    echo ""
    log_success "모든 작업이 완료되었습니다!"
}

# 스크립트 실행
main "$@"
