@echo off
chcp 65001 >nul
echo ========================================
echo  树洞后端一键启动 (本地模式)
echo ========================================
echo.

echo [1/3] 创建数据目录...
if not exist "E:\data\db" mkdir "E:\data\db"

echo [2/3] 启动 MongoDB...
start "MongoDB" cmd /k "mongod --dbpath E:\data\db"

timeout /t 3 /nobreak >nul

echo [3/3] 启动后端服务...
start "TreeHole Backend" cmd /k "cd /d %~dp0 && npm start"

echo.
echo ========================================
echo  启动完成！
echo ========================================
echo.
echo 后端地址: http://10.234.109.132:3001
echo.
echo 手机连接时确保在同一 WiFi 网络
echo 关闭此窗口不会停止服务
echo ========================================
echo.
pause
