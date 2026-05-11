$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "../..")

function Test-Docker {
  try {
    docker version --format '{{.Server.Version}}' | Out-Null
    return $true
  } catch {
    return $false
  }
}

if (-not (Test-Docker)) {
  Write-Error "Docker 不可用：请安装并启动 Docker Desktop。"
}

if (!(Test-Path ".env")) { Copy-Item ".env.example" ".env" }

docker compose up -d --build
docker compose ps

$backendPort = 8080
if (Test-Path ".env") {
  Get-Content ".env" | ForEach-Object {
    if ($_ -match '^\s*BACKEND_PORT\s*=\s*(\d+)') { $script:backendPort = $matches[1] }
  }
}

$healthUrl = "http://127.0.0.1:$backendPort/actuator/health"
Write-Host "等待后端就绪: $healthUrl （最长约 10 分钟，Oracle 首次初始化较慢）"
$ok = $false
for ($i = 0; $i -lt 200; $i++) {
  try {
    $r = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
    if ($r.Content -match 'UP') { $ok = $true; break }
  } catch {}
  Start-Sleep -Seconds 3
}
if ($ok) { Write-Host "后端健康检查已通过。" } else { Write-Warning "后端未在预期时间内就绪，请执行 docker compose logs timer-backend" }

$httpPort = 80
if (Test-Path ".env") {
  Get-Content ".env" | ForEach-Object {
    if ($_ -match '^\s*HTTP_PORT\s*=\s*(\d+)') { $script:httpPort = $matches[1] }
  }
}

Write-Host ""
Write-Host "======== CCEC 访问入口 ========"
Write-Host "工位屏:     http://localhost:$httpPort/?station=A601"
Write-Host "管理控制台: http://localhost:$httpPort/admin/login.html"
Write-Host "后端健康:   $healthUrl"
Write-Host "================================"
