$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "未检测到 Docker，请先安装 Docker 24+ 和 Docker Compose 2+。"
}

docker compose version | Out-Null

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "已自动创建 .env。首次部署如使用自己的 GHCR 命名空间，请编辑 .env 的 IMAGE_NAMESPACE。"
}

$Mode = if ($args.Count -gt 0) { $args[0] } else { "lite" }
$coreServices = @(
    "nacos",
    "redis",
    "auth-service",
    "api-gateway",
    "agent-workspace",
    "admin-portal"
)
$fullOnlyServices = @(
    "zookeeper",
    "kafka",
    "minio",
    "customer-service",
    "agent-service",
    "call-service",
    "ivr-service",
    "recording-service",
    "ticket-service",
    "quality-service",
    "report-service",
    "notification-service",
    "call-distribution-engine",
    "ws-gateway"
)

function Wait-ContainerReady {
    param(
        [string]$Container,
        [string]$Label,
        [int]$TimeoutSeconds = 180
    )

    Write-Host "等待 $Label 就绪..."
    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds) {
        $status = docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" $Container 2>$null
        if ($status -eq "healthy") {
            Write-Host "$Label 已就绪。"
            return
        }

        if (-not $status) {
            $status = "unknown"
        }
        Write-Host "$Label 当前状态：$status，继续等待..."
        Start-Sleep -Seconds 5
        $elapsed += 5
    }

    throw "$Label 等待超时，请执行：docker logs --tail=80 $Container"
}

switch ($Mode) {
    "lite" {
        Write-Host "使用轻量模式启动：只启动页面和登录必需服务。"
        Write-Host "如需启动录音、质检、通知、WebSocket、Kafka 等完整服务，请执行：.\scripts\docker-up.ps1 full"
        Write-Host "正在停止完整模式才需要的重型服务，避免旧容器反复重启占用资源..."
        docker compose stop @fullOnlyServices
        docker compose rm -f -s @fullOnlyServices
        docker compose pull @coreServices
        docker compose up -d nacos redis
        Wait-ContainerReady "qianniu-nacos" "Nacos" 240
        Wait-ContainerReady "qianniu-redis" "Redis" 120
        docker compose up -d --force-recreate auth-service
        Wait-ContainerReady "qianniu-auth-service" "认证服务" 240
        docker compose up -d --force-recreate api-gateway
        Wait-ContainerReady "qianniu-api-gateway" "API网关" 240
        docker compose up -d --force-recreate --no-deps agent-workspace admin-portal
        docker compose ps @coreServices
        break
    }
    "core" {
        Write-Host "使用轻量模式启动：只启动页面和登录必需服务。"
        Write-Host "如需启动录音、质检、通知、WebSocket、Kafka 等完整服务，请执行：.\scripts\docker-up.ps1 full"
        Write-Host "正在停止完整模式才需要的重型服务，避免旧容器反复重启占用资源..."
        docker compose stop @fullOnlyServices
        docker compose rm -f -s @fullOnlyServices
        docker compose pull @coreServices
        docker compose up -d nacos redis
        Wait-ContainerReady "qianniu-nacos" "Nacos" 240
        Wait-ContainerReady "qianniu-redis" "Redis" 120
        docker compose up -d --force-recreate auth-service
        Wait-ContainerReady "qianniu-auth-service" "认证服务" 240
        docker compose up -d --force-recreate api-gateway
        Wait-ContainerReady "qianniu-api-gateway" "API网关" 240
        docker compose up -d --force-recreate --no-deps agent-workspace admin-portal
        docker compose ps @coreServices
        break
    }
    "full" {
        Write-Host "使用完整模式启动：会启动全部中间件和全部微服务。"
        docker compose pull
        docker compose up -d
        docker compose ps
        break
    }
    default {
        throw "未知模式：$Mode。用法：.\scripts\docker-up.ps1 [lite|full]"
    }
}

function Get-EnvValue {
    param(
        [string]$Name,
        [string]$DefaultValue
    )

    if (-not (Test-Path ".env")) {
        return $DefaultValue
    }

    $line = Get-Content ".env" | Where-Object { $_ -match "^\s*$Name=" } | Select-Object -Last 1
    if (-not $line) {
        return $DefaultValue
    }

    return ($line -replace "^\s*$Name=", "").Trim()
}

$agentPort = Get-EnvValue "AGENT_WORKSPACE_PORT" "5173"
$adminPort = Get-EnvValue "ADMIN_PORTAL_PORT" "5174"
$adminUser = Get-EnvValue "ADMIN_USERNAME" "admin"
$adminPassword = Get-EnvValue "ADMIN_PASSWORD" "Admin@2025"

Write-Host ""
Write-Host "启动完成。默认访问地址："
Write-Host "座席工作台：http://localhost:$agentPort"
Write-Host "管理后台：http://localhost:$adminPort"
Write-Host "默认账号：$adminUser / $adminPassword"
