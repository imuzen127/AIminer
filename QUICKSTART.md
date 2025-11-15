# AIminer - Quick Start Guide

Complete setup in 3 steps.

## Prerequisites

- Minecraft server (Paper 1.21) running or ready to start
- SSH/terminal access to server
- Python 3.8+ on server

## Step 1: Upload to Server

Upload the entire `AIminer` directory to your server.

**Using git (recommended):**
```bash
git clone https://github.com/imuzen127/AIminer.git
cd AIminer
```

**Or upload via SFTP/SCP:**
```bash
scp -r AIminer/ user@server:/path/to/minecraft/
```

## Step 2: Run Deployment Script

This will deploy everything automatically:

```bash
cd AIminer
chmod +x deploy.sh
./deploy.sh
```

**What it does:**
- ✓ Checks environment (Python, pip, etc.)
- ✓ Builds and copies plugin to server
- ✓ Copies datapack to world folder
- ✓ Installs AI server dependencies
- ✓ Downloads AI model from Hugging Face
- ✓ Creates configuration files
- ✓ Runs health checks

**You will be asked for:**
- Minecraft server directory (auto-detected if in parent folder)
- Hugging Face model repository (e.g., `imuzen127/your-model`)

## Step 3: Start Everything

### Start AI Server

```bash
cd ai-brain
./start.sh
```

Wait for: `Model loaded successfully!`

### Start Minecraft Server

```bash
cd /path/to/minecraft
./start.sh  # or your server start command
```

Wait for server to fully start.

### Test In-Game

1. Join your Minecraft server
2. Run command: `/bot start`
3. Wait 10 seconds

Check server console for:
```
[AIminer] AI processing completed in 1234ms - Task added: true
[AIminer] New task generated: MINE_WOOD (ID: 1)
```

## Troubleshooting

### If something goes wrong

Run diagnostics:
```bash
cd AIminer
chmod +x diagnostics.sh
./diagnostics.sh
```

This will:
- Check all system components
- Verify file structure
- Test AI server connection
- Analyze Minecraft logs
- Show specific fixes for any issues

### Common Issues

**"Connection refused" in Minecraft logs**
```bash
# AI server not running
cd ai-brain
./start.sh
```

**"Model not found"**
```bash
# Download model manually
cd ai-brain
python3 -m huggingface_hub.commands.download_cli \
    --repo-id YOUR_REPO \
    --local-dir models
```

**"Plugin not found"**
```bash
# Rebuild and copy plugin
cd plugin/AIminer
./gradlew build
cp build/libs/AIminer-1.6-SNAPSHOT.jar /path/to/server/plugins/
```

## Configuration

Edit `plugins/AIminer/config.yml` on Minecraft server:

```yaml
ai-server:
  url: "http://localhost:8080"  # Change if AI server is on different machine
  enabled: true
  interval: 10  # AI processing interval (seconds)

vision:
  scan-radius: 10  # Block scanning radius
  update-interval: 5  # Vision update interval (seconds)
```

Restart Minecraft server after changes.

## File Locations

After deployment:

```
Minecraft Server/
├── plugins/
│   └── AIminer-1.6-SNAPSHOT.jar
├── plugins/AIminer/
│   ├── config.yml
│   └── brain.json
└── world/
    └── datapacks/
        └── manekinwalk_datapack/

AIminer/ (anywhere on server)
└── ai-brain/
    ├── api_server.py
    ├── models/
    │   └── model.gguf
    └── start.sh
```

## Verification Checklist

After setup, verify:

- [ ] AI server responds: `curl http://localhost:8080/health`
- [ ] Minecraft server started without errors
- [ ] Plugin loaded: Check logs for `[AIminer] plugin has been enabled!`
- [ ] Config created: `plugins/AIminer/config.yml` exists
- [ ] Brain file created: `plugins/AIminer/brain.json` exists
- [ ] `/bot start` command works in-game
- [ ] AI processing logs appear every 10 seconds

## Logs to Monitor

**AI Server:**
```bash
# When running ./start.sh, you'll see:
Model loaded successfully in 15.23 seconds!
INFO:     Uvicorn running on http://0.0.0.0:8080
```

**Minecraft Server:**
```
[AIminer] AIminer plugin has been enabled!
[AIminer] Task executor started.
[AIminer] Vision update system started.
[AIminer] AI processing system started (server: http://localhost:8080)
```

**During operation:**
```
[AIminer] Starting AI brain processing...
[AIminer] AI processing completed in 1234ms (server: 1150ms) - Task added: true
[AIminer] New task generated: MINE_WOOD (ID: 1)
[AIminer] Executing task #1: MINE_WOOD
```

## Performance

- **AI Server startup:** 10-30 seconds (one-time model load)
- **AI processing:** 1-3 seconds per decision
- **Vision scanning:** Every 5 seconds
- **Memory usage:** 4-12 GB (depends on model size)

## Getting Help

1. **Run diagnostics:** `./diagnostics.sh`
2. **Check AI server logs:** Look at terminal where `./start.sh` is running
3. **Check Minecraft logs:** `logs/latest.log` in server directory
4. **Verify files:** Make sure all components are deployed correctly

## Advanced

### Running AI Server as Background Service

```bash
# Using screen
screen -S aiminer-api
cd ai-brain
./start.sh
# Press Ctrl+A, D to detach

# Reattach later
screen -r aiminer-api
```

### Changing AI Model

1. Download new model to `ai-brain/models/`
2. Update `ai-brain/.env`:
   ```
   AIMINER_MODEL_PATH=./models/new-model.gguf
   ```
3. Restart AI server

### Multiple Minecraft Servers

Each server needs its own `config.yml` pointing to the AI server:
```yaml
ai-server:
  url: "http://10.0.0.5:8080"  # Shared AI server IP
```

## Next Steps

- Experiment with different AI models
- Adjust processing intervals in config
- Monitor bot behavior and refine prompts
- Build custom datapacks for new actions

---

For detailed documentation:
- AI Server: `AI_SERVER_SETUP.md`
- Development: `conversation-logs/`
