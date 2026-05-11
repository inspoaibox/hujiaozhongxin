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

docker compose pull
docker compose up -d
docker compose ps

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
