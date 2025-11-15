#!/bin/bash
# AIminer AI Server - Automated Setup Script
# Run this on your server to complete AI setup

set -e  # Exit on error

echo "=================================================="
echo "  AIminer AI Server - Automated Setup"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Check Python
echo -e "${YELLOW}[1/6] Checking Python installation...${NC}"
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 is not installed${NC}"
    echo "Please install Python 3.8 or higher first"
    exit 1
fi

PYTHON_VERSION=$(python3 --version | awk '{print $2}')
echo -e "${GREEN}✓ Python ${PYTHON_VERSION} found${NC}"

# Step 2: Check pip
echo -e "\n${YELLOW}[2/6] Checking pip...${NC}"
if ! python3 -m pip --version &> /dev/null; then
    echo -e "${RED}Error: pip is not installed${NC}"
    echo "Installing pip..."
    python3 -m ensurepip --upgrade
fi
echo -e "${GREEN}✓ pip is available${NC}"

# Step 3: Install Python dependencies
echo -e "\n${YELLOW}[3/6] Installing Python dependencies...${NC}"
echo "This may take a few minutes (llama-cpp-python needs to compile)..."
python3 -m pip install -r requirements.txt --upgrade

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Dependencies installed successfully${NC}"
else
    echo -e "${RED}✗ Failed to install dependencies${NC}"
    exit 1
fi

# Step 4: Setup model directory
echo -e "\n${YELLOW}[4/6] Setting up model directory...${NC}"
mkdir -p models
echo -e "${GREEN}✓ Models directory created${NC}"

# Step 5: Download model from Hugging Face
echo -e "\n${YELLOW}[5/6] Downloading AI model from Hugging Face...${NC}"

# Ask user for model repository
echo ""
echo "Enter your Hugging Face model repository name"
echo "Example: imuzen127/minecraft-bot-model"
read -p "Repository: " MODEL_REPO

if [ -z "$MODEL_REPO" ]; then
    echo -e "${YELLOW}⚠ Skipping model download${NC}"
    echo "You can download it later with:"
    echo "  python3 -m huggingface_hub.commands.download_cli --repo-id YOUR_REPO --local-dir models"
else
    echo "Downloading from: $MODEL_REPO"
    echo "This may take several minutes depending on model size..."

    python3 -m huggingface_hub.commands.download_cli \
        --repo-id "$MODEL_REPO" \
        --local-dir models \
        --local-dir-use-symlinks False

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Model downloaded successfully${NC}"
    else
        echo -e "${RED}✗ Model download failed${NC}"
        echo "You can download it manually later"
    fi
fi

# Step 6: Configure environment
echo -e "\n${YELLOW}[6/6] Configuring environment...${NC}"

# Find the .gguf file
GGUF_FILE=$(find models -name "*.gguf" -type f | head -n 1)

if [ -z "$GGUF_FILE" ]; then
    echo -e "${YELLOW}⚠ No .gguf model file found in models/${NC}"
    echo "Please place your GGUF model file in the models/ directory"
    GGUF_FILE="./models/model.gguf"
else
    echo -e "${GREEN}✓ Found model: $GGUF_FILE${NC}"
fi

# Create environment file
cat > .env << EOF
# AIminer AI Server Configuration
AIMINER_MODEL_PATH=$GGUF_FILE
AIMINER_PORT=8080
EOF

echo -e "${GREEN}✓ Environment configured (.env file created)${NC}"

# Create convenient start script
cat > start.sh << 'EOF'
#!/bin/bash
# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "Starting AIminer AI Server..."
echo "Model: $AIMINER_MODEL_PATH"
echo "Port: $AIMINER_PORT"
echo ""

python3 api_server.py
EOF

chmod +x start.sh

echo ""
echo -e "${GREEN}=================================================="
echo "  ✓ Setup Complete!"
echo "==================================================${NC}"
echo ""
echo "To start the AI server, run:"
echo -e "${GREEN}  ./start.sh${NC}"
echo ""
echo "Or manually:"
echo -e "${GREEN}  export AIMINER_MODEL_PATH=$GGUF_FILE${NC}"
echo -e "${GREEN}  export AIMINER_PORT=8080${NC}"
echo -e "${GREEN}  python3 api_server.py${NC}"
echo ""
echo "Server will be available at: http://localhost:8080"
echo ""
echo "Run './start.sh' to start the server now!"
echo ""
