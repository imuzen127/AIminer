#!/bin/bash
# Start AIminer Brain API Server

# Set model path (change this to your model location)
export AIMINER_MODEL_PATH="${AIMINER_MODEL_PATH:-./models/model.gguf}"
export AIMINER_PORT="${AIMINER_PORT:-8080}"

echo "Starting AIminer Brain API Server..."
echo "Model: $AIMINER_MODEL_PATH"
echo "Port: $AIMINER_PORT"
echo ""

python3 api_server.py
