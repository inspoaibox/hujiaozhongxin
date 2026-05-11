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

docker compose pull
docker compose up -d
docker compose ps

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
