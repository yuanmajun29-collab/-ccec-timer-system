Write-Host "Start Redis/Sentinel first, then Oracle connectivity check."
Start-Process -FilePath "D:\CCEC-Timer\app\timer-backend.jar"
Start-Process -FilePath "D:\CCEC-Timer\app\plc_collector.exe"
Write-Host "Services started. Check logs under D:\CCEC-Timer\logs"
