@echo off
echo ================================
echo 协作文档系统 - 本地启动脚本
echo ================================
echo.

:: 检查Maven
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Maven未安装，请先安装Maven
    pause
    exit /b 1
)

:: 检查Node.js
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Node.js未安装，请先安装Node.js
    pause
    exit /b 1
)

echo [1/3] 编译后端项目...
cd /d %~dp0
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Maven编译失败
    pause
    exit /b 1
)

echo.
echo [2/3] 安装前端依赖...
cd frontend
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo [错误] npm install失败
    pause
    exit /b 1
)

echo.
echo [3/3] 编译完成！
echo.
echo ================================
echo 请按以下顺序启动服务：
echo ================================
echo.
echo 1. 启动基础设施（MySQL、Redis、Nacos）:
echo    docker-compose up -d mysql redis nacos
echo.
echo 2. 等待Nacos启动后（约30秒），访问: http://localhost:8848/nacos
echo    默认账号: nacos / nacos
echo.
echo 3. 在不同终端启动各微服务:
echo    - cd gateway-service ^&^& mvn spring-boot:run
echo    - cd user-service ^&^& mvn spring-boot:run
echo    - cd document-service ^&^& mvn spring-boot:run
echo    - cd collaboration-service ^&^& mvn spring-boot:run
echo    - cd history-service ^&^& mvn spring-boot:run
echo.
echo 4. 启动前端:
echo    cd frontend ^&^& npm run dev
echo.
echo 5. 访问系统: http://localhost:3000
echo    测试账号: admin / 123456 或 test / 123456
echo.
pause
