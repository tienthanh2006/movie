# Chạy toàn bộ hệ thống ở chế độ DEV trên Windows (PowerShell).
# Yêu cầu: Docker Desktop, JDK 17+, Maven (lệnh mvn) hoặc có sẵn mvnw.cmd trong thư mục này.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$mvn = if (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }

Write-Host ">> Bật MySQL, Redis, RabbitMQ..."
docker compose up -d
Write-Host ">> Chờ MySQL sẵn sàng..."
do { Start-Sleep -Seconds 3; $h = docker inspect -f "{{.State.Health.Status}}" cinema-mysql } while ($h -ne "healthy")

Write-Host ">> Build (bỏ qua test)..."
& $mvn -B clean install -DskipTests

$services = "user-service","movie-service","venue-service","booking-service","notification-service"
foreach ($s in $services) {
    Write-Host ">> Khởi động $s trong cửa sổ mới"
    Start-Process powershell -ArgumentList "-NoExit","-Command",
        "`$env:SPRING_PROFILES_ACTIVE='dev'; Set-Location '$PSScriptRoot'; $mvn -pl $s spring-boot:run"
}
Write-Host "Xong. Swagger ví dụ: http://localhost:8084/swagger-ui/index.html (booking), 8082 (movie), 8083 (venue)"
Write-Host "Admin: admin / Admin@123456   |   RabbitMQ UI: http://localhost:15672 (cinema / cinema123)"
