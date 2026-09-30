# Build WAR, triển khai lên Tomcat và chạy ở cửa sổ hiện tại (Ctrl+C để dừng).
#   .\run.ps1            build + test + chạy
#   .\run.ps1 -SkipTests build nhanh không chạy test
# Đường dẫn công cụ lấy từ biến môi trường JAVA_HOME, MAVEN_HOME, CATALINA_HOME nếu máy đã đặt;
# chưa đặt thì dùng vị trí cài mặc định bên dưới.
param([switch]$SkipTests)

$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12' }
$mavenHome = if ($env:MAVEN_HOME) { $env:MAVEN_HOME } else { "$env:USERPROFILE\tools\apache-maven-3.9.16" }
if (-not $env:CATALINA_HOME) { $env:CATALINA_HOME = "$env:USERPROFILE\tools\apache-tomcat-10.1.60" }
$mvn = "$mavenHome\bin\mvn.cmd"

foreach ($p in $env:JAVA_HOME, $mvn, $env:CATALINA_HOME) {
    if (-not (Test-Path $p)) { throw "Không tìm thấy $p — xem mục Cài đặt trong README.md" }
}

Set-Location $PSScriptRoot
$mvnArgs = @('-q', '-B', 'clean', 'package')
if ($SkipTests) { $mvnArgs += '-DskipTests' }
& $mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw 'Build thất bại' }

Remove-Item "$env:CATALINA_HOME\webapps\oms" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item 'target\oms.war' "$env:CATALINA_HOME\webapps\oms.war" -Force

Write-Host 'Mở http://localhost:8080/oms sau khi thấy "Server startup"' -ForegroundColor Green
& "$env:CATALINA_HOME\bin\catalina.bat" run
