@echo off
echo 启动基础设施服务（MySQL、Redis、Nacos）...
docker-compose up -d mysql redis nacos
echo.
echo 等待服务启动...
timeout /t 30 /nobreak
echo.
echo 基础设施启动完成！
echo Nacos控制台: http://localhost:8848/nacos (nacos/nacos)
echo MySQL: localhost:3306 (root/root)
echo Redis: localhost:6379
echo.
pause
