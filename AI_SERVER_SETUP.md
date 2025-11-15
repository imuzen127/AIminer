# AI Server Setup Guide

This guide explains how to set up the AI Brain API Server for real-time AI processing.

## Architecture

```
[Minecraft Server] <--HTTP--> [AI Brain API Server] <--GGUF Model--> [AI Processing]
   Paper 1.21                   FastAPI (Python)              llama-cpp-python
   port 25565                   port 8080
```

## Prerequisites

- Python 3.8 or higher
- Minecraft server (Paper 1.21) running
- AI model file (GGUF format) - already uploaded to Hugging Face

## Installation Steps

### 1. Install Python Dependencies

```bash
cd ai-brain
pip install -r requirements.txt
```

**Note:** `llama-cpp-python` compilation may take a few minutes.

### 2. Download AI Model

Download your model from Hugging Face:

```bash
# Set your model repository
export MODEL_REPO="your-username/your-model-name"

# Create models directory
mkdir -p models

# Download the model
python -m huggingface_hub download \
    --repo-id $MODEL_REPO \
    --local-dir models \
    --local-dir-use-symlinks False
```

Example:
```bash
python -m huggingface_hub download \
    --repo-id imuzen127/minecraft-bot-model \
    --local-dir models \
    --local-dir-use-symlinks False
```

### 3. Configure Environment Variables

**Linux/Mac:**
```bash
export AIMINER_MODEL_PATH="./models/your-model.gguf"
export AIMINER_PORT=8080
```

**Windows (PowerShell):**
```powershell
$env:AIMINER_MODEL_PATH="./models/your-model.gguf"
$env:AIMINER_PORT=8080
```

**Windows (CMD):**
```cmd
set AIMINER_MODEL_PATH=./models/your-model.gguf
set AIMINER_PORT=8080
```

### 4. Start the API Server

**Linux/Mac:**
```bash
cd ai-brain
./start_server.sh
```

**Windows:**
```cmd
cd ai-brain
start_server.bat
```

**Manual start:**
```bash
cd ai-brain
python api_server.py
```

### 5. Verify Server Health

Open your browser and visit:
- Health check: http://localhost:8080/health
- API docs: http://localhost:8080/docs

You should see:
```json
{
  "status": "healthy",
  "model_loaded": true,
  "model_load_time": "15.23s"
}
```

### 6. Configure Minecraft Plugin

Edit `plugins/AIminer/config.yml`:

```yaml
ai-server:
  url: "http://localhost:8080"
  enabled: true
  interval: 10
```

**If AI server is on a different machine:**
```yaml
ai-server:
  url: "http://192.168.1.100:8080"  # Replace with your server IP
  enabled: true
  interval: 10
```

### 7. Start Minecraft Server

Start your Paper 1.21 server with the AIminer plugin installed.

Check logs for:
```
[AIminer] AI server is ready!
[AIminer] AI processing system started (server: http://localhost:8080)
```

## Testing

1. Join your Minecraft server
2. Run command: `/bot start`
3. The bot will be summoned
4. Wait 10 seconds for first AI processing
5. Check server logs for AI responses

Expected logs:
```
[AIminer] Starting AI brain processing...
[AIminer] AI processing completed in 1234ms (server: 1150ms) - Task added: true
[AIminer] New task generated: MINE_WOOD (ID: 1)
```

## Performance Optimization

### Model Loading Time
- **First startup:** 10-30 seconds (model loads into memory)
- **Subsequent requests:** 1-3 seconds (model stays in memory)

### Faster Inference
To reduce response time to ~1 second:

1. **Use a smaller model:**
   - 3B-7B parameter models are faster than 13B+
   - Quantized models (Q4, Q5) are faster than full precision

2. **Enable GPU acceleration:**
   ```bash
   # Install with GPU support
   pip install llama-cpp-python --force-reinstall --no-cache-dir \
       --extra-index-url https://abetlen.github.io/llama-cpp-python/whl/cu121
   ```

3. **Adjust inference parameters** in `api_server.py`:
   ```python
   response = llm_model(
       prompt,
       max_tokens=64,      # Reduce from 128
       temperature=0.5,    # Lower = faster
       top_p=0.8,
       stop=["\n"],
       echo=False
   )
   ```

## Troubleshooting

### "Model not loaded" Error

Check:
1. Model path is correct: `echo $AIMINER_MODEL_PATH`
2. File exists: `ls -lh models/`
3. Server logs show model loading errors

### "Connection refused" Error

Check:
1. API server is running: `curl http://localhost:8080/health`
2. Port 8080 is not blocked by firewall
3. Plugin config has correct URL

### "Processing takes too long"

Check:
1. Model size (use smaller model)
2. CPU/GPU usage during inference
3. Reduce `max_tokens` in API server

## API Endpoints

### POST /api/brain
Process brain data and get AI decision

**Request:**
```json
{
  "brain_data": {
    "rules": {...},
    "vision": {...},
    "memory": {...},
    "tasks": [...]
  }
}
```

**Response:**
```json
{
  "brain_data": {...},
  "processing_time_ms": 1234,
  "task_added": true,
  "task": {
    "id": 1,
    "type": "MINE_WOOD",
    "status": "PENDING",
    "parameters": {"x": -13, "y": -55, "z": 47}
  }
}
```

### GET /health
Check server health

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "model_load_time": "15.23s"
}
```

## Running as a Service (Production)

### Using systemd (Linux)

Create `/etc/systemd/system/aiminer-api.service`:

```ini
[Unit]
Description=AIminer Brain API Server
After=network.target

[Service]
Type=simple
User=minecraft
WorkingDirectory=/home/minecraft/AIminer/ai-brain
Environment="AIMINER_MODEL_PATH=/home/minecraft/AIminer/models/model.gguf"
Environment="AIMINER_PORT=8080"
ExecStart=/usr/bin/python3 /home/minecraft/AIminer/ai-brain/api_server.py
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable aiminer-api
sudo systemctl start aiminer-api
sudo systemctl status aiminer-api
```

## Next Steps

- Experiment with different AI models
- Adjust processing interval in config.yml
- Monitor AI responses and tune prompts
- Add custom tasks to the system
