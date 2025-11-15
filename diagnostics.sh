#!/bin/bash
# AIminer - Comprehensive Diagnostics Script
# Run this to diagnose issues with AIminer system

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=================================================="
echo "  AIminer - System Diagnostics"
echo "==================================================${NC}"
echo ""

ISSUES_FOUND=0

# Function to check and report
check() {
    local name="$1"
    local command="$2"
    local fix="$3"

    echo -n "Checking $name... "
    if eval "$command" >/dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}FAILED${NC}"
        if [ -n "$fix" ]; then
            echo "  Fix: $fix"
        fi
        ((ISSUES_FOUND++))
        return 1
    fi
}

# =============================================================================
# System Checks
# =============================================================================
echo -e "${BLUE}=== System Components ===${NC}"

check "Python 3" "command -v python3" "Install: sudo apt install python3"
check "pip" "python3 -m pip --version" "Install: sudo apt install python3-pip"
check "curl" "command -v curl" "Install: sudo apt install curl"

# =============================================================================
# Python Dependencies
# =============================================================================
echo ""
echo -e "${BLUE}=== Python Dependencies ===${NC}"

check "fastapi" "python3 -c 'import fastapi'" "pip install fastapi"
check "uvicorn" "python3 -c 'import uvicorn'" "pip install uvicorn"
check "llama-cpp-python" "python3 -c 'import llama_cpp'" "pip install llama-cpp-python"
check "pydantic" "python3 -c 'import pydantic'" "pip install pydantic"
check "huggingface-hub" "python3 -c 'import huggingface_hub'" "pip install huggingface-hub"

# =============================================================================
# File Structure
# =============================================================================
echo ""
echo -e "${BLUE}=== File Structure ===${NC}"

check "Plugin JAR" "[ -f plugin/AIminer/build/libs/AIminer-1.6-SNAPSHOT.jar ]" \
    "Build: cd plugin/AIminer && ./gradlew build"

check "Datapack" "[ -d datapack/manekinwalk_datapack ]" \
    "Clone repo properly"

check "AI server script" "[ -f ai-brain/api_server.py ]" \
    "Clone repo properly"

check "Prompt template" "[ -f ai-brain/prompt_template.py ]" \
    "Clone repo properly"

# =============================================================================
# AI Server
# =============================================================================
echo ""
echo -e "${BLUE}=== AI Server ===${NC}"

cd ai-brain 2>/dev/null || true

# Check for model
GGUF_COUNT=$(find models -name "*.gguf" -type f 2>/dev/null | wc -l)
if [ $GGUF_COUNT -gt 0 ]; then
    echo -e "AI Model... ${GREEN}OK${NC} ($GGUF_COUNT found)"
    find models -name "*.gguf" -type f | while read f; do
        SIZE=$(du -h "$f" | cut -f1)
        echo "  - $(basename "$f") ($SIZE)"
    done
else
    echo -e "AI Model... ${RED}FAILED${NC}"
    echo "  Fix: Download model with:"
    echo "    python3 -m huggingface_hub.commands.download_cli --repo-id YOUR_REPO --local-dir models"
    ((ISSUES_FOUND++))
fi

# Check .env
if [ -f ".env" ]; then
    echo -e "Configuration (.env)... ${GREEN}OK${NC}"
    cat .env | grep -v '^#' | while read line; do
        echo "  $line"
    done
else
    echo -e "Configuration (.env)... ${YELLOW}MISSING${NC}"
    echo "  Will use environment variables"
fi

# Check if server is running
echo -n "API Server status... "
if curl -s http://localhost:8080/health >/dev/null 2>&1; then
    echo -e "${GREEN}RUNNING${NC}"

    # Get detailed health info
    HEALTH=$(curl -s http://localhost:8080/health)
    echo "$HEALTH" | python3 -m json.tool 2>/dev/null | sed 's/^/  /'
else
    echo -e "${YELLOW}NOT RUNNING${NC}"
    echo "  Start with: cd ai-brain && ./start.sh"
fi

cd .. 2>/dev/null || true

# =============================================================================
# Minecraft Server
# =============================================================================
echo ""
echo -e "${BLUE}=== Minecraft Server ===${NC}"

# Find MC server
MC_DIR=""
for dir in .. ../.. ../../..; do
    if [ -f "$dir/paper.jar" ] || [ -f "$dir/server.jar" ]; then
        MC_DIR="$dir"
        break
    fi
done

if [ -n "$MC_DIR" ]; then
    MC_DIR=$(cd "$MC_DIR" && pwd)
    echo -e "Server directory... ${GREEN}OK${NC}"
    echo "  Path: $MC_DIR"

    # Check plugin
    if [ -f "$MC_DIR/plugins/AIminer-1.6-SNAPSHOT.jar" ]; then
        echo -e "Plugin installed... ${GREEN}OK${NC}"
        PLUGIN_SIZE=$(du -h "$MC_DIR/plugins/AIminer-1.6-SNAPSHOT.jar" | cut -f1)
        echo "  Size: $PLUGIN_SIZE"
    else
        echo -e "Plugin installed... ${RED}MISSING${NC}"
        echo "  Fix: Copy plugin/AIminer/build/libs/AIminer-1.6-SNAPSHOT.jar to $MC_DIR/plugins/"
        ((ISSUES_FOUND++))
    fi

    # Check datapack
    if [ -d "$MC_DIR/world/datapacks/manekinwalk_datapack" ]; then
        echo -e "Datapack installed... ${GREEN}OK${NC}"
    else
        echo -e "Datapack installed... ${YELLOW}MISSING${NC}"
        echo "  Fix: Copy datapack/manekinwalk_datapack to $MC_DIR/world/datapacks/"
    fi

    # Check config
    if [ -f "$MC_DIR/plugins/AIminer/config.yml" ]; then
        echo -e "Plugin config... ${GREEN}OK${NC}"

        AI_URL=$(grep "url:" "$MC_DIR/plugins/AIminer/config.yml" | awk '{print $2}' | tr -d '"')
        AI_ENABLED=$(grep "enabled:" "$MC_DIR/plugins/AIminer/config.yml" | awk '{print $2}')

        echo "  AI Server URL: $AI_URL"
        echo "  AI Enabled: $AI_ENABLED"
    else
        echo -e "Plugin config... ${YELLOW}NOT CREATED YET${NC}"
        echo "  Will be created on first server start"
    fi

    # Check if server is running
    echo -n "Server status... "
    if pgrep -f "paper.jar\|server.jar" >/dev/null; then
        echo -e "${GREEN}RUNNING${NC}"

        # Try to find plugin in logs
        if [ -f "$MC_DIR/logs/latest.log" ]; then
            if grep -q "AIminer" "$MC_DIR/logs/latest.log" 2>/dev/null; then
                echo -e "  Plugin loaded... ${GREEN}YES${NC}"

                # Check for errors
                ERROR_COUNT=$(grep -i "aiminer.*error\|aiminer.*exception" "$MC_DIR/logs/latest.log" 2>/dev/null | wc -l)
                if [ $ERROR_COUNT -gt 0 ]; then
                    echo -e "  ${RED}Found $ERROR_COUNT error(s) in logs${NC}"
                    echo "  Recent errors:"
                    grep -i "aiminer.*error\|aiminer.*exception" "$MC_DIR/logs/latest.log" | tail -n 5 | sed 's/^/    /'
                    ((ISSUES_FOUND++))
                fi
            fi
        fi
    else
        echo -e "${YELLOW}NOT RUNNING${NC}"
        echo "  Start server to test plugin"
    fi

else
    echo -e "Server directory... ${YELLOW}NOT FOUND${NC}"
    echo "  Please ensure Minecraft server is set up"
fi

# =============================================================================
# Network Checks
# =============================================================================
echo ""
echo -e "${BLUE}=== Network ===${NC}"

check "Port 8080 available" "! lsof -i :8080 >/dev/null 2>&1 || curl -s http://localhost:8080/health >/dev/null" \
    "Port 8080 is in use by another process or AI server is running"

check "Internet connectivity" "curl -s https://www.google.com >/dev/null" \
    "Check internet connection"

# =============================================================================
# Common Issues
# =============================================================================
echo ""
echo -e "${BLUE}=== Common Issues ===${NC}"

# Check for common log errors
if [ -n "$MC_DIR" ] && [ -f "$MC_DIR/logs/latest.log" ]; then
    # Connection refused
    if grep -q "Connection refused.*8080" "$MC_DIR/logs/latest.log" 2>/dev/null; then
        echo -e "${RED}✗${NC} AI server connection refused"
        echo "  Fix: Start AI server with: cd ai-brain && ./start.sh"
        ((ISSUES_FOUND++))
    fi

    # Model not found
    if grep -q "Model file not found" "$MC_DIR/logs/latest.log" 2>/dev/null; then
        echo -e "${RED}✗${NC} AI model not found"
        echo "  Fix: Download model to ai-brain/models/"
        ((ISSUES_FOUND++))
    fi
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo -e "${BLUE}=================================================="
echo "  Summary"
echo "==================================================${NC}"

if [ $ISSUES_FOUND -eq 0 ]; then
    echo -e "${GREEN}✓ No issues found!${NC}"
    echo ""
    echo "System is ready to use:"
    echo "1. AI server: cd ai-brain && ./start.sh"
    echo "2. Minecraft: Start server"
    echo "3. In-game: /bot start"
else
    echo -e "${RED}Found $ISSUES_FOUND issue(s)${NC}"
    echo ""
    echo "Fix the issues above and run diagnostics again"
fi

echo ""
