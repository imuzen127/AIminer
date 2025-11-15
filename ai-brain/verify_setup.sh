#!/bin/bash
# Verify AIminer AI Server Setup

echo "=================================================="
echo "  AIminer AI Server - Setup Verification"
echo "=================================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

SUCCESS=0
FAILED=0

# Check 1: Python
echo -n "Checking Python... "
if command -v python3 &> /dev/null; then
    VERSION=$(python3 --version | awk '{print $2}')
    echo -e "${GREEN}✓${NC} Python $VERSION"
    ((SUCCESS++))
else
    echo -e "${RED}✗${NC} Not found"
    ((FAILED++))
fi

# Check 2: Required packages
echo -n "Checking fastapi... "
if python3 -c "import fastapi" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Installed"
    ((SUCCESS++))
else
    echo -e "${RED}✗${NC} Not installed"
    ((FAILED++))
fi

echo -n "Checking llama-cpp-python... "
if python3 -c "import llama_cpp" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Installed"
    ((SUCCESS++))
else
    echo -e "${RED}✗${NC} Not installed"
    ((FAILED++))
fi

echo -n "Checking uvicorn... "
if python3 -c "import uvicorn" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Installed"
    ((SUCCESS++))
else
    echo -e "${RED}✗${NC} Not installed"
    ((FAILED++))
fi

# Check 3: Files
echo -n "Checking api_server.py... "
if [ -f "api_server.py" ]; then
    echo -e "${GREEN}✓${NC} Found"
    ((SUCCESS++))
else
    echo -e "${RED}✗${NC} Not found"
    ((FAILED++))
fi

echo -n "Checking prompt_template.py... "
if [ -f "prompt_template.py" ]; then
    echo -e "${GREEN}✓${NC} Found"
    ((SUCCESS++))
else
    echo -e "${RED}✗${NC} Not found"
    ((FAILED++))
fi

# Check 4: Model directory
echo -n "Checking models directory... "
if [ -d "models" ]; then
    echo -e "${GREEN}✓${NC} Found"
    ((SUCCESS++))

    # Check for .gguf file
    echo -n "Checking for .gguf model... "
    GGUF_COUNT=$(find models -name "*.gguf" -type f | wc -l)
    if [ $GGUF_COUNT -gt 0 ]; then
        echo -e "${GREEN}✓${NC} Found $GGUF_COUNT model(s)"
        find models -name "*.gguf" -type f | while read -r file; do
            SIZE=$(du -h "$file" | cut -f1)
            echo "    - $(basename "$file") ($SIZE)"
        done
        ((SUCCESS++))
    else
        echo -e "${YELLOW}⚠${NC} No .gguf model found"
        echo "    Download model with: python3 -m huggingface_hub.commands.download_cli --repo-id YOUR_REPO --local-dir models"
        ((FAILED++))
    fi
else
    echo -e "${RED}✗${NC} Not found"
    ((FAILED++))
fi

# Check 5: Environment configuration
echo -n "Checking .env file... "
if [ -f ".env" ]; then
    echo -e "${GREEN}✓${NC} Found"
    ((SUCCESS++))

    if [ -f ".env" ]; then
        source .env
        echo "    Model path: $AIMINER_MODEL_PATH"
        echo "    Port: $AIMINER_PORT"
    fi
else
    echo -e "${YELLOW}⚠${NC} Not found (optional)"
fi

# Summary
echo ""
echo "=================================================="
echo "  Verification Summary"
echo "=================================================="
echo -e "Passed: ${GREEN}$SUCCESS${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Setup is complete!${NC}"
    echo ""
    echo "To start the server, run:"
    echo "  ./start.sh"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Setup is incomplete${NC}"
    echo ""
    echo "Please run setup.sh to complete the setup"
    echo "  ./setup.sh"
    echo ""
    exit 1
fi
