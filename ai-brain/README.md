# AIminer AI Brain Server

Fast API server for real-time AI processing of Minecraft bot decisions.

## Quick Setup (Server)

### 1. Upload Files to Server

Upload the entire `ai-brain/` directory to your Minecraft server.

### 2. Run Setup Script

**Linux:**
```bash
cd ai-brain
chmod +x setup.sh
./setup.sh
```

**Windows:**
```cmd
cd ai-brain
setup.bat
```

The script will:
- ✓ Check Python installation
- ✓ Install dependencies (FastAPI, llama-cpp-python, etc.)
- ✓ Create models directory
- ✓ Download AI model from Hugging Face (if you provide repo name)
- ✓ Configure environment variables
- ✓ Create start script

### 3. Start the Server

**Linux:**
```bash
./start.sh
```

**Windows:**
```cmd
start.bat
```

### 4. Verify Server is Running

Open browser: http://localhost:8080/health

You should see:
```json
{
  "status": "healthy",
  "model_loaded": true,
  "model_load_time": "15.23s"
}
```

## Manual Setup (if automated setup fails)

### Install Dependencies
```bash
pip install -r requirements.txt
```

### Download Model
```bash
python -m huggingface_hub.commands.download_cli \
    --repo-id YOUR_USERNAME/YOUR_MODEL \
    --local-dir models
```

### Set Environment Variables

**Linux/Mac:**
```bash
export AIMINER_MODEL_PATH=./models/your-model.gguf
export AIMINER_PORT=8080
```

**Windows:**
```cmd
set AIMINER_MODEL_PATH=./models/your-model.gguf
set AIMINER_PORT=8080
```

### Start Server
```bash
python api_server.py
```

## Verify Setup

Run the verification script:

**Linux:**
```bash
chmod +x verify_setup.sh
./verify_setup.sh
```

This will check:
- Python installation
- Required packages
- Model files
- Configuration

## Troubleshooting

### "Python not found"
Install Python 3.8+ first:
- **Ubuntu/Debian:** `sudo apt install python3 python3-pip`
- **Windows:** Download from https://python.org

### "llama-cpp-python installation failed"
This package needs to compile C++ code. Install build tools:
- **Ubuntu/Debian:** `sudo apt install build-essential`
- **Windows:** Install Visual Studio Build Tools

### "Model not found"
Make sure you have a `.gguf` file in the `models/` directory.
Download from Hugging Face:
```bash
python -m huggingface_hub.commands.download_cli \
    --repo-id YOUR_REPO \
    --local-dir models
```

### "Port 8080 already in use"
Change the port:
```bash
export AIMINER_PORT=8081
```

Or edit `.env` file:
```
AIMINER_PORT=8081
```

## Performance

- **First startup:** 10-30 seconds (model loads into memory)
- **Subsequent requests:** 1-3 seconds
- **Memory usage:** Depends on model size (typically 4-12 GB)

## API Endpoints

### GET /health
Health check

### GET /
Basic status

### POST /api/brain
Process brain data and get AI decision

**Request:**
```json
{
  "brain_data": { ... }
}
```

**Response:**
```json
{
  "brain_data": { ... updated ... },
  "processing_time_ms": 1234,
  "task_added": true,
  "task": { ... }
}
```

## Running as Background Service

### Linux (systemd)

Create `/etc/systemd/system/aiminer-api.service`:
```ini
[Unit]
Description=AIminer Brain API
After=network.target

[Service]
Type=simple
User=minecraft
WorkingDirectory=/path/to/ai-brain
EnvironmentFile=/path/to/ai-brain/.env
ExecStart=/usr/bin/python3 /path/to/ai-brain/api_server.py
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Enable:
```bash
sudo systemctl enable aiminer-api
sudo systemctl start aiminer-api
```

### Using screen (Simple)

```bash
screen -S aiminer-api
./start.sh
# Press Ctrl+A, then D to detach
```

Reattach:
```bash
screen -r aiminer-api
```

## Files

- `api_server.py` - Main FastAPI server
- `prompt_template.py` - AI prompt generation
- `brain_processor.py` - Standalone processor (legacy)
- `requirements.txt` - Python dependencies
- `setup.sh` / `setup.bat` - Automated setup
- `start.sh` / `start.bat` - Server startup
- `verify_setup.sh` - Setup verification

## Support

For detailed setup guide, see: `../AI_SERVER_SETUP.md`
