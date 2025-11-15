#!/bin/bash
# AIminer - Complete Deployment and Diagnostics Script
# Run this on your Minecraft server to deploy and verify everything

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0
WARNINGS=0

echo -e "${BLUE}=================================================="
echo "  AIminer - Complete Deployment & Diagnostics"
echo "==================================================${NC}"
echo ""

# Function to print section header
section() {
    echo ""
    echo -e "${BLUE}### $1 ###${NC}"
}

# Function to print success
success() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED++))
}

# Function to print error
error() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED++))
}

# Function to print warning
warning() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

# =============================================================================
# STEP 1: Environment Check
# =============================================================================
section "Step 1: Environment Check"

# Check if we're in the right directory
if [ ! -f "README.md" ] || [ ! -d "plugin" ]; then
    error "Not in AIminer directory. Please cd to AIminer first."
    exit 1
fi
success "Running from AIminer directory"

# Find Minecraft server directory
MC_SERVER_DIR=""
if [ -f "../server.jar" ] || [ -f "../paper.jar" ]; then
    MC_SERVER_DIR=".."
elif [ -f "../../server.jar" ] || [ -f "../../paper.jar" ]; then
    MC_SERVER_DIR="../.."
else
    warning "Minecraft server not found in parent directories"
    echo "Please specify Minecraft server directory:"
    read -p "Path: " MC_SERVER_DIR
    if [ ! -d "$MC_SERVER_DIR" ]; then
        error "Directory does not exist: $MC_SERVER_DIR"
        exit 1
    fi
fi

if [ -n "$MC_SERVER_DIR" ]; then
    MC_SERVER_DIR=$(cd "$MC_SERVER_DIR" && pwd)
    success "Found Minecraft server at: $MC_SERVER_DIR"
fi

# =============================================================================
# STEP 2: Plugin Deployment
# =============================================================================
section "Step 2: Plugin Deployment"

PLUGIN_JAR="plugin/AIminer/build/libs/AIminer-1.6-SNAPSHOT.jar"

if [ ! -f "$PLUGIN_JAR" ]; then
    warning "Plugin JAR not built yet"
    echo "Building plugin..."
    cd plugin/AIminer
    ./gradlew clean build
    cd ../..

    if [ ! -f "$PLUGIN_JAR" ]; then
        error "Plugin build failed"
        exit 1
    fi
    success "Plugin built successfully"
else
    success "Plugin JAR found"
fi

# Copy to server
if [ -n "$MC_SERVER_DIR" ]; then
    mkdir -p "$MC_SERVER_DIR/plugins"
    cp "$PLUGIN_JAR" "$MC_SERVER_DIR/plugins/"
    success "Plugin copied to server: $MC_SERVER_DIR/plugins/"

    # Check if old config exists
    if [ -f "$MC_SERVER_DIR/plugins/AIminer/config.yml" ]; then
        warning "Existing config found - will be preserved"
    fi
fi

# =============================================================================
# STEP 3: Datapack Deployment
# =============================================================================
section "Step 3: Datapack Deployment"

DATAPACK_DIR="datapack/manekinwalk_datapack"

if [ ! -d "$DATAPACK_DIR" ]; then
    error "Datapack not found: $DATAPACK_DIR"
else
    success "Datapack found"

    if [ -n "$MC_SERVER_DIR" ]; then
        # Find world directory
        WORLD_DIR=""
        if [ -d "$MC_SERVER_DIR/world" ]; then
            WORLD_DIR="$MC_SERVER_DIR/world"
        else
            warning "World directory not found"
            echo "Datapack must be manually copied to: world/datapacks/"
        fi

        if [ -n "$WORLD_DIR" ]; then
            mkdir -p "$WORLD_DIR/datapacks"
            cp -r "$DATAPACK_DIR" "$WORLD_DIR/datapacks/"
            success "Datapack copied to: $WORLD_DIR/datapacks/"
        fi
    fi
fi

# =============================================================================
# STEP 4: AI Server Setup
# =============================================================================
section "Step 4: AI Server Setup"

cd ai-brain

# Check Python
if ! command -v python3 &> /dev/null; then
    error "Python 3 not installed"
    echo "Install with: sudo apt install python3 python3-pip"
else
    PYTHON_VERSION=$(python3 --version | awk '{print $2}')
    success "Python $PYTHON_VERSION installed"
fi

# Check if dependencies are installed
echo "Checking Python dependencies..."
DEPS_OK=true

if ! python3 -c "import fastapi" 2>/dev/null; then
    warning "fastapi not installed"
    DEPS_OK=false
fi

if ! python3 -c "import llama_cpp" 2>/dev/null; then
    warning "llama-cpp-python not installed"
    DEPS_OK=false
fi

if ! python3 -c "import uvicorn" 2>/dev/null; then
    warning "uvicorn not installed"
    DEPS_OK=false
fi

if [ "$DEPS_OK" = false ]; then
    echo ""
    echo "Installing missing dependencies..."
    python3 -m pip install -r requirements.txt
    success "Dependencies installed"
else
    success "All Python dependencies installed"
fi

# Check for model
GGUF_FILES=$(find models -name "*.gguf" -type f 2>/dev/null | wc -l)
if [ $GGUF_FILES -eq 0 ]; then
    warning "No AI model found in models/"
    echo ""
    echo "Download model from Hugging Face?"
    read -p "Repository (or press Enter to skip): " MODEL_REPO

    if [ -n "$MODEL_REPO" ]; then
        echo "Downloading model..."
        python3 -m huggingface_hub.commands.download_cli \
            --repo-id "$MODEL_REPO" \
            --local-dir models \
            --local-dir-use-symlinks False
        success "Model downloaded"
    fi
else
    success "Found $GGUF_FILES model file(s)"
fi

# Create .env if not exists
if [ ! -f ".env" ]; then
    GGUF_FILE=$(find models -name "*.gguf" -type f | head -n 1)
    if [ -n "$GGUF_FILE" ]; then
        cat > .env << EOF
AIMINER_MODEL_PATH=$GGUF_FILE
AIMINER_PORT=8080
EOF
        success "Created .env configuration"
    fi
fi

# Create start script if not exists
if [ ! -f "start.sh" ]; then
    cat > start.sh << 'EOF'
#!/bin/bash
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi
python3 api_server.py
EOF
    chmod +x start.sh
    success "Created start.sh"
fi

cd ..

# =============================================================================
# STEP 5: Configuration Check
# =============================================================================
section "Step 5: Configuration Check"

if [ -n "$MC_SERVER_DIR" ] && [ -f "$MC_SERVER_DIR/plugins/AIminer/config.yml" ]; then
    success "Plugin config exists"

    # Check AI server URL
    AI_URL=$(grep "url:" "$MC_SERVER_DIR/plugins/AIminer/config.yml" | awk '{print $2}' | tr -d '"')
    if [ -n "$AI_URL" ]; then
        echo "    AI Server URL: $AI_URL"
    fi
else
    warning "Plugin config will be created on first run"
    echo "    Default AI server: http://localhost:8080"
fi

# =============================================================================
# STEP 6: Health Checks
# =============================================================================
section "Step 6: Health Checks"

# Check if AI server is running
echo -n "Checking AI server... "
if curl -s http://localhost:8080/health >/dev/null 2>&1; then
    echo -e "${GREEN}Running${NC}"
    success "AI server is healthy"
else
    echo -e "${YELLOW}Not running${NC}"
    warning "AI server not started yet"
    echo "    Start with: cd ai-brain && ./start.sh"
fi

# Check if Minecraft server is running
echo -n "Checking Minecraft server... "
if [ -n "$MC_SERVER_DIR" ] && pgrep -f "paper.jar\|server.jar" >/dev/null; then
    echo -e "${GREEN}Running${NC}"
    success "Minecraft server is running"
else
    echo -e "${YELLOW}Not running${NC}"
    warning "Minecraft server not started yet"
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo -e "${BLUE}=================================================="
echo "  Deployment Summary"
echo "==================================================${NC}"
echo -e "Passed:   ${GREEN}$PASSED${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"
echo -e "Failed:   ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Deployment complete!${NC}"
    echo ""
    echo "Next steps:"
    echo ""
    echo "1. Start AI server (if not running):"
    echo "   ${GREEN}cd ai-brain && ./start.sh${NC}"
    echo ""
    echo "2. Start/restart Minecraft server"
    echo ""
    echo "3. In Minecraft, run:"
    echo "   ${GREEN}/bot start${NC}"
    echo ""
    echo "4. Check logs for:"
    echo "   - [AIminer] AI processing system started"
    echo "   - [AIminer] Vision update system started"
    echo ""
else
    echo -e "${RED}✗ Deployment incomplete${NC}"
    echo ""
    echo "Please fix the errors above and run again"
    exit 1
fi
