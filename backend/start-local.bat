@echo off
chcp 65001 >nul
echo ========================================
echo  树洞后端一键启动 (本地模式)
echo ========================================
echo.

echo [1/3] 创建数据目录...
set "MONGO_DBPATH=D:\AppData\MongoDB"
if not exist "%MONGO_DBPATH%" mkdir "%MONGO_DBPATH%"

echo [2/3] 启动 MongoDB...
set "MONGOD_CMD=mongod"
where mongod >nul 2>nul
if errorlevel 1 (
    if exist "D:\AppData\MongoDB\bin\mongod.exe" (
        set "MONGOD_CMD=D:\AppData\MongoDB\bin\mongod.exe"
    ) else if exist "D:\AppData\MongoDB\mongod.exe" (
        set "MONGOD_CMD=D:\AppData\MongoDB\mongod.exe"
    ) else if exist "D:\AppData\MongoDB\Server\8.0\bin\mongod.exe" (
        set "MONGOD_CMD=D:\AppData\MongoDB\Server\8.0\bin\mongod.exe"
    ) else if exist "D:\AppData\MongoDB\Server\7.0\bin\mongod.exe" (
        set "MONGOD_CMD=D:\AppData\MongoDB\Server\7.0\bin\mongod.exe"
    ) else (
        if exist "C:\Program Files\MongoDB\Server\8.0\bin\mongod.exe" (
            set "MONGOD_CMD=C:\Program Files\MongoDB\Server\8.0\bin\mongod.exe"
        ) else if exist "C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe" (
            set "MONGOD_CMD=C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe"
        ) else (
            echo 未检测到 MongoDB，请先安装 MongoDB Community Server
            echo 下载地址: https://www.mongodb.com/try/download/community
            pause
            exit /b 1
        )
    )
)
start "MongoDB" cmd /k ""%MONGOD_CMD%" --dbpath "%MONGO_DBPATH%""

timeout /t 3 /nobreak >nul

echo [3/3] 启动后端服务...
start "TreeHole Backend" cmd /k "cd /d %~dp0 && npm start"

echo.
echo ========================================
echo  启动完成！
echo ========================================
echo.
echo 本机后端地址: http://127.0.0.1:3001
echo 手机访问地址(当前): http://10.234.171.102:3001
echo 手机访问地址(备用): http://10.234.171.102:3001
echo.
echo 手机连接时确保在同一 WiFi 网络
echo 关闭此窗口不会停止服务
echo ========================================
echo.
pause
