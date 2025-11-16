@echo off
REM Start AIminer Brain API Server with .env configuration

REM Load environment variables from .env file
if exist .env (
    for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
        set %%a=%%b
    )
)

REM Set defaults if not in .env
if "%AIMINER_MODEL_PATH%"=="" set AIMINER_MODEL_PATH=models/model.gguf
if "%AIMINER_PORT%"=="" set AIMINER_PORT=8080

echo Starting AIminer AI Server...
echo Model: %AIMINER_MODEL_PATH%
echo Port: %AIMINER_PORT%
echo.

python api_server.py
