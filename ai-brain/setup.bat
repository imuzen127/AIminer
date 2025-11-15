@echo off
REM AIminer AI Server - Automated Setup Script (Windows)
REM Run this on your server to complete AI setup

echo ==================================================
echo   AIminer AI Server - Automated Setup
echo ==================================================
echo.

REM Step 1: Check Python
echo [1/6] Checking Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo Error: Python 3 is not installed
    echo Please install Python 3.8 or higher first
    pause
    exit /b 1
)

for /f "tokens=2" %%i in ('python --version') do set PYTHON_VERSION=%%i
echo OK: Python %PYTHON_VERSION% found
echo.

REM Step 2: Check pip
echo [2/6] Checking pip...
python -m pip --version >nul 2>&1
if errorlevel 1 (
    echo Error: pip is not installed
    echo Installing pip...
    python -m ensurepip --upgrade
)
echo OK: pip is available
echo.

REM Step 3: Install Python dependencies
echo [3/6] Installing Python dependencies...
echo This may take a few minutes (llama-cpp-python needs to compile)...
python -m pip install -r requirements.txt --upgrade

if errorlevel 1 (
    echo Error: Failed to install dependencies
    pause
    exit /b 1
)
echo OK: Dependencies installed successfully
echo.

REM Step 4: Setup model directory
echo [4/6] Setting up model directory...
if not exist "models" mkdir models
echo OK: Models directory created
echo.

REM Step 5: Download model
echo [5/6] Downloading AI model from Hugging Face...
echo.
echo Enter your Hugging Face model repository name
echo Example: imuzen127/minecraft-bot-model
set /p MODEL_REPO="Repository: "

if "%MODEL_REPO%"=="" (
    echo Warning: Skipping model download
    echo You can download it later with:
    echo   python -m huggingface_hub.commands.download_cli --repo-id YOUR_REPO --local-dir models
) else (
    echo Downloading from: %MODEL_REPO%
    echo This may take several minutes depending on model size...

    python -m huggingface_hub.commands.download_cli --repo-id %MODEL_REPO% --local-dir models --local-dir-use-symlinks False

    if errorlevel 1 (
        echo Error: Model download failed
        echo You can download it manually later
    ) else (
        echo OK: Model downloaded successfully
    )
)
echo.

REM Step 6: Configure environment
echo [6/6] Configuring environment...

REM Find .gguf file
for /r "models" %%f in (*.gguf) do (
    set GGUF_FILE=%%f
    goto :found_model
)

echo Warning: No .gguf model file found in models/
echo Please place your GGUF model file in the models/ directory
set GGUF_FILE=models\model.gguf
goto :create_env

:found_model
echo OK: Found model: %GGUF_FILE%

:create_env
REM Create environment file
(
echo # AIminer AI Server Configuration
echo AIMINER_MODEL_PATH=%GGUF_FILE%
echo AIMINER_PORT=8080
) > .env

echo OK: Environment configured (.env file created)

REM Create convenient start script
(
echo @echo off
echo REM Load environment variables
echo for /f "tokens=1,2 delims==" %%%%a in (.env^) do (
echo     if not "%%%%a"=="#" set %%%%a=%%%%b
echo ^)
echo.
echo echo Starting AIminer AI Server...
echo echo Model: %%AIMINER_MODEL_PATH%%
echo echo Port: %%AIMINER_PORT%%
echo echo.
echo.
echo python api_server.py
) > start.bat

echo.
echo ==================================================
echo   Setup Complete!
echo ==================================================
echo.
echo To start the AI server, run:
echo   start.bat
echo.
echo Or manually:
echo   set AIMINER_MODEL_PATH=%GGUF_FILE%
echo   set AIMINER_PORT=8080
echo   python api_server.py
echo.
echo Server will be available at: http://localhost:8080
echo.
pause
