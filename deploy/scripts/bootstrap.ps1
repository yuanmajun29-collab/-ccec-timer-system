$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "../..")
if (!(Test-Path ".env")) { Copy-Item ".env.example" ".env" }
docker compose up -d --build
docker compose ps
Write-Host "Open: http://localhost/?station=A601"
