#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v docker >/dev/null 2>&1; then
  echo "未检测到 Docker，请先安装 Docker 24+ 和 Docker Compose 2+。"
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "未检测到 docker compose，请先安装 Docker Compose 2+。"
  exit 1
fi

if [ ! -f .env ]; then
  cp .env.example .env
  echo "已自动创建 .env。首次部署如使用自己的 GHCR 命名空间，请编辑 .env 的 IMAGE_NAMESPACE。"
fi

mode="${1:-lite}"
core_services=(
  nacos
  redis
  auth-service
  api-gateway
  agent-workspace
  admin-portal
)
full_only_services=(
  zookeeper
  kafka
  minio
  customer-service
  agent-service
  call-service
  ivr-service
  recording-service
  ticket-service
  quality-service
  report-service
  notification-service
  call-distribution-engine
  ws-gateway
)

wait_healthy() {
  local container="$1"
  local label="$2"
  local timeout_seconds="${3:-180}"
  local elapsed=0

  echo "等待 ${label} 就绪..."
  while [ "$elapsed" -lt "$timeout_seconds" ]; do
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    if [ "$status" = "healthy" ]; then
      echo "${label} 已就绪。"
      return 0
    fi
    sleep 5
    elapsed=$((elapsed + 5))
  done

  echo "${label} 等待超时，请执行：docker logs --tail=80 ${container}"
  return 1
}

case "$mode" in
  lite|core)
    echo "使用轻量模式启动：只启动页面和登录必需服务。"
    echo "如需启动录音、质检、通知、WebSocket、Kafka 等完整服务，请执行：bash scripts/docker-up.sh full"
    echo "正在停止完整模式才需要的重型服务，避免旧容器反复重启占用资源..."
    docker compose stop "${full_only_services[@]}" || true
    docker compose rm -f -s "${full_only_services[@]}" || true
    docker compose pull "${core_services[@]}"
    docker compose up -d --force-recreate nacos redis
    wait_healthy qianniu-nacos Nacos 240
    wait_healthy qianniu-redis Redis 120
    docker compose up -d --force-recreate auth-service
    wait_healthy qianniu-auth-service 认证服务 240
    docker compose up -d --force-recreate api-gateway
    wait_healthy qianniu-api-gateway API网关 240
    docker compose up -d --force-recreate --no-deps agent-workspace admin-portal
    docker compose ps "${core_services[@]}"
    ;;
  full)
    echo "使用完整模式启动：会启动全部中间件和全部微服务。"
    docker compose pull
    docker compose up -d
    docker compose ps
    ;;
  *)
    echo "未知模式：$mode"
    echo "用法：bash scripts/docker-up.sh [lite|full]"
    exit 1
    ;;
esac

get_env_value() {
  local key="$1"
  local fallback="$2"
  local value
  value="$(grep -E "^${key}=" .env 2>/dev/null | tail -n 1 | cut -d '=' -f 2- || true)"
  if [ -n "$value" ]; then
    echo "$value"
  else
    echo "$fallback"
  fi
}

agent_port="$(get_env_value AGENT_WORKSPACE_PORT 5173)"
admin_port="$(get_env_value ADMIN_PORTAL_PORT 5174)"
admin_user="$(get_env_value ADMIN_USERNAME admin)"
admin_password="$(get_env_value ADMIN_PASSWORD 'Admin@2025')"

echo ""
echo "启动完成。默认访问地址："
echo "座席工作台：http://localhost:${agent_port}"
echo "管理后台：http://localhost:${admin_port}"
echo "默认账号：${admin_user} / ${admin_password}"
