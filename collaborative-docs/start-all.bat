@echo off
echo 启动所有服务（Docker方式）...
docker-compose up -d --build
echo.
echo 所有服务启动完成！
echo.
echo 访问地址:
echo - 前端: http://localhost:3000
echo - 网关: http://localhost:8080
echo - Nacos: http://localhost:8848/nacos
echo.
echo 测试账号: admin / 123456
echo.
pause
