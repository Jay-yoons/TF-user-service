#!/bin/bash

# 🚀 Store Service 배포 스크립트
# Team-FOG Store Service를 AWS ECS에 배포합니다.

set -e

# 환경변수 설정
export AWS_REGION="ap-northeast-2"
export ECR_REPOSITORY_URI="$(aws ecr describe-repositories --repository-names team-fog-store-service --query 'repositories[0].repositoryUri' --output text)"
export ECS_CLUSTER_NAME="team-fog-cluster"
export ECS_SERVICE_NAME="store-service"
export ECS_TASK_DEFINITION_NAME="store-service-task"

echo "🚀 Store Service 배포 시작..."
echo "📍 AWS Region: $AWS_REGION"
echo "📦 ECR Repository: $ECR_REPOSITORY_URI"
echo "🏗️ ECS Cluster: $ECS_CLUSTER_NAME"

# 1. Docker 이미지 빌드
echo "🔨 Docker 이미지 빌드 중..."
docker build -t team-fog-store-service:latest .

# 2. ECR 로그인
echo "🔐 ECR 로그인 중..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPOSITORY_URI

# 3. 이미지 태그 및 푸시
echo "📤 Docker 이미지 푸시 중..."
docker tag team-fog-store-service:latest $ECR_REPOSITORY_URI:latest
docker push $ECR_REPOSITORY_URI:latest

# 4. ECS Task Definition 등록
echo "📋 ECS Task Definition 등록 중..."
aws ecs register-task-definition --cli-input-json file://aws/store-service-task-definition.json

# 5. ECS 서비스 업데이트
echo "🔄 ECS 서비스 업데이트 중..."
aws ecs update-service \
    --cluster $ECS_CLUSTER_NAME \
    --service $ECS_SERVICE_NAME \
    --force-new-deployment

# 6. 배포 상태 확인
echo "📊 배포 상태 확인 중..."
aws ecs describe-services \
    --cluster $ECS_CLUSTER_NAME \
    --services $ECS_SERVICE_NAME \
    --query 'services[0].deployments[0].status' \
    --output text

echo "✅ Store Service 배포 완료!"
echo "🌐 서비스 URL: http://store-service.internal:8081"
echo "📈 CloudWatch 로그: /ecs/store-service"
