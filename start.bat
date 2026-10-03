@echo off
chcp 65001 > nul
setlocal

REM 一键启动学生选课管理系统（需预先安装 JDK/Maven/Node.js，并已启动 MySQL、Redis、RabbitMQ）
cd /d "%~dp0"

echo ========================================
echo  启动学生选课管理系统
echo ========================================
echo.

where mvn >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Maven，请先安装 Maven 并配置 PATH
    pause
    exit /b 1
)

where node >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Node.js，请先安装 Node.js 并配置 PATH
    pause
    exit /b 1
)

echo [1/3] 启动后端服务...
start "Backend" cmd /k "cd /d "%~dp0course-selection-backend" && mvn spring-boot:run"

echo.
echo [2/3] 等待后端服务启动...
timeout /t 15 /nobreak > nul

echo.
echo [3/3] 启动前端服务...
start "Frontend" cmd /k "cd /d "%~dp0course-selection-front" && npm install && npm run serve"

echo.
echo ========================================
echo  启动命令已发出
echo  后端 API:  http://localhost:8080
echo  前端页面:  http://localhost:3000
echo ========================================
echo  若首次启动失败，请检查 MySQL / Redis / RabbitMQ 是否已启动
echo.
pause
