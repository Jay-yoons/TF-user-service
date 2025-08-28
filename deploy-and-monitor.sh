#!/bin/bash

# Team FOG User Service ECS 배포 및 Health Check 모니터링 스크립트

# 설정
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION="ap-northeast-2"
CLUSTER_NAME="team-fog-cluster"
SERVICE_NAME="user-service"
TASK_DEFINITION_FILE="aws/ecs-task-definition-user-service.json"
LOG_GROUP="/ecs/user-service"

echo "🚀 Team FOG User Service ECS 배포 및 모니터링 시작..."

# 1. Task Definition 업데이트
echo "📝 Task Definition 업데이트 중..."
sed -i "s/YOUR_ACCOUNT_ID/$ACCOUNT_ID/g" $TASK_DEFINITION_FILE
sed -i "s/YOUR_DB_HOST/10.0.13.41/g" $TASK_DEFINITION_FILE

# 2. Task Definition 등록
echo "📋 Task Definition 등록 중..."
TASK_DEFINITION_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://$TASK_DEFINITION_FILE \
    --region $REGION \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

echo "✅ Task Definition 등록 완료: $TASK_DEFINITION_ARN"

# 3. ECS Service 업데이트 (또는 생성)
echo "🔄 ECS Service 업데이트 중..."
aws ecs update-service \
    --cluster $CLUSTER_NAME \
    --service $SERVICE_NAME \
    --task-definition $TASK_DEFINITION_ARN \
    --force-new-deployment \
    --region $REGION

echo "✅ ECS Service 업데이트 완료"

# 4. 배포 상태 모니터링
echo "📊 배포 상태 모니터링 중..."
aws ecs wait services-stable \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $REGION

echo "✅ 배포 완료!"

# 5. Health Check 로그 모니터링
echo "🔍 Health Check 로그 모니터링 시작..."
echo "로그 그룹: $LOG_GROUP"
echo "실시간 로그를 보려면 다음 명령어를 사용하세요:"
echo "aws logs tail $LOG_GROUP --follow --region $REGION"
echo ""

# 6. 최근 Health Check 로그 조회
echo "📋 최근 Health Check 로그 조회 중..."
aws logs describe-log-streams \
    --log-group-name $LOG_GROUP \
    --order-by LastEventTime \
    --descending \
    --max-items 5 \
    --region $REGION \
    --query 'logStreams[0].logStreamName' \
    --output text | xargs -I {} aws logs get-log-events \
    --log-group-name $LOG_GROUP \
    --log-stream-name {} \
    --region $REGION \
    --query 'events[?contains(message, `Health Check`)].{timestamp: timestamp, message: message}' \
    --output table

# 7. 상세 Health Check 엔드포인트 테스트
echo ""
echo "🧪 Health Check 엔드포인트 테스트..."
echo "기본 Health Check:"
curl -s http://localhost:8082/health | jq .

echo ""
echo "상세 Health Check:"
curl -s http://localhost:8082/health/detailed | jq .

echo ""
echo "Health Check 로그 테스트:"
curl -s http://localhost:8082/health/log-test

# 8. 실시간 로그 모니터링 옵션
echo ""
echo "📺 실시간 로그 모니터링 옵션:"
echo "1. 전체 로그: aws logs tail $LOG_GROUP --follow --region $REGION"
echo "2. Health Check만: aws logs tail $LOG_GROUP --follow --filter-pattern 'Health Check' --region $REGION"
echo "3. 에러만: aws logs tail $LOG_GROUP --follow --filter-pattern 'ERROR' --region $REGION"
echo "4. 특정 시간: aws logs tail $LOG_GROUP --since 1h --region $REGION"

# 9. CloudWatch 대시보드 링크
echo ""
echo "📊 CloudWatch 대시보드:"
echo "https://console.aws.amazon.com/cloudwatch/home?region=$REGION#logsV2:log-groups/log-group/$LOG_GROUP"

echo ""
echo "✅ User Service ECS 배포 및 모니터링 설정 완료!"
