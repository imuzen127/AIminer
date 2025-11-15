@echo off
REM Start AIminer Brain API Server (Windows)

REM Set model path (change this to your model location)
if "%AIMINER_MODEL_PATH%"=="" set AIMINER_MODEL_PATH=./models/model.gguf
if "%AIMINER_PORT%"=="" set AIMINER_PORT=8080

echo Starting AIminer Brain API Server...
echo Model: %AIMINER_MODEL_PATH%
echo Port: %AIMINER_PORT%
echo.

python api_server.py
