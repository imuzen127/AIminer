#!/bin/bash
# Start AIminer Brain API Server with .env configuration

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Set defaults if not in .env
if [ -z "$AIMINER_MODEL_PATH" ]; then
    export AIMINER_MODEL_PATH="models/model.gguf"
fi

if [ -z "$AIMINER_PORT" ]; then
    export AIMINER_PORT=8080
fi

echo "Starting AIminer AI Server..."
echo "Model: $AIMINER_MODEL_PATH"
echo "Port: $AIMINER_PORT"
echo ""

python3 api_server.py
