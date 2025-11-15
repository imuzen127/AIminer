#!/usr/bin/env python3
"""
FastAPI Server for AIminer AI Brain Processing
Provides real-time HTTP API for Minecraft plugin
"""

import json
import time
import os
from pathlib import Path
from typing import Optional
import logging

try:
    from fastapi import FastAPI, HTTPException
    from fastapi.responses import JSONResponse
    from pydantic import BaseModel
except ImportError:
    print("Error: FastAPI is not installed.")
    print("Install it with: pip install fastapi uvicorn")
    exit(1)

try:
    from llama_cpp import Llama
except ImportError:
    print("Error: llama-cpp-python is not installed.")
    print("Install it with: pip install llama-cpp-python")
    exit(1)

from prompt_template import generate_prompt, parse_ai_response

# Logging setup
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("AIminer-API")

# FastAPI app
app = FastAPI(
    title="AIminer Brain API",
    description="AI processing server for Minecraft bot",
    version="1.0.0"
)

# Global model instance (loaded once at startup)
llm_model: Optional[Llama] = None
model_load_time: float = 0.0


class BrainRequest(BaseModel):
    """Request body for brain processing"""
    brain_data: dict


class BrainResponse(BaseModel):
    """Response body for brain processing"""
    brain_data: dict
    processing_time_ms: int
    task_added: bool
    task: Optional[dict] = None


@app.on_event("startup")
async def load_model():
    """Load AI model on server startup"""
    global llm_model, model_load_time

    # Get model path from environment variable or default
    model_path = os.getenv("AIMINER_MODEL_PATH", "./models/model.gguf")

    logger.info(f"Loading AI model from: {model_path}")
    start_time = time.time()

    if not Path(model_path).exists():
        logger.error(f"Model file not found: {model_path}")
        logger.error("Please set AIMINER_MODEL_PATH environment variable")
        # Don't exit - allow server to start but API will return errors
        return

    try:
        llm_model = Llama(
            model_path=str(model_path),
            n_ctx=4096,  # Context window
            n_threads=os.cpu_count() or 4,  # Use all CPU threads
            n_gpu_layers=-1,  # Use GPU if available
            verbose=False
        )
        model_load_time = time.time() - start_time
        logger.info(f"Model loaded successfully in {model_load_time:.2f} seconds!")
        logger.info(f"Server ready to process requests")
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        logger.error("Server will start but API endpoints will return errors")


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "running",
        "model_loaded": llm_model is not None,
        "model_load_time": f"{model_load_time:.2f}s" if llm_model else "not loaded"
    }


@app.get("/health")
async def health_check():
    """Detailed health check"""
    if llm_model is None:
        return JSONResponse(
            status_code=503,
            content={
                "status": "unhealthy",
                "reason": "Model not loaded",
                "model_path": os.getenv("AIMINER_MODEL_PATH", "./models/model.gguf")
            }
        )

    return {
        "status": "healthy",
        "model_loaded": True,
        "model_load_time": f"{model_load_time:.2f}s"
    }


@app.post("/api/brain", response_model=BrainResponse)
async def process_brain(request: BrainRequest):
    """
    Process brain data and return updated brain with new task

    Request body:
    {
        "brain_data": { ... full brain.json content ... }
    }

    Response:
    {
        "brain_data": { ... updated brain.json ... },
        "processing_time_ms": 1234,
        "task_added": true,
        "task": { ... generated task ... }
    }
    """
    if llm_model is None:
        raise HTTPException(
            status_code=503,
            detail="AI model not loaded. Please check server logs."
        )

    start_time = time.time()
    brain_data = request.brain_data

    try:
        # Generate prompt from brain data
        prompt = generate_prompt(brain_data)
        logger.debug(f"Generated prompt (length: {len(prompt)} chars)")

        # Run inference
        logger.info("Running AI inference...")
        response = llm_model(
            prompt,
            max_tokens=128,  # Max tokens to generate
            temperature=0.7,  # Creativity (0.0-1.0)
            top_p=0.9,       # Nucleus sampling
            stop=["\n"],     # Stop at newline
            echo=False       # Don't echo the prompt
        )

        # Extract text from response
        ai_output = response['choices'][0]['text'].strip()
        logger.info(f"AI Response: {ai_output}")

        # Parse response into task
        task = parse_ai_response(ai_output)

        if task is None:
            logger.warning("Could not parse AI response into valid task")
            processing_time = int((time.time() - start_time) * 1000)
            return BrainResponse(
                brain_data=brain_data,
                processing_time_ms=processing_time,
                task_added=False,
                task=None
            )

        # Add timestamp
        task['createdAt'] = int(time.time() * 1000)

        # Initialize tasks array if not exists
        if 'tasks' not in brain_data:
            brain_data['tasks'] = []

        # Generate task ID
        existing_ids = [t.get('id', 0) for t in brain_data['tasks']]
        task['id'] = max(existing_ids, default=0) + 1

        # Add task to brain data
        brain_data['tasks'].append(task)

        processing_time = int((time.time() - start_time) * 1000)
        logger.info(f"Task generated in {processing_time}ms: {task['type']}")

        return BrainResponse(
            brain_data=brain_data,
            processing_time_ms=processing_time,
            task_added=True,
            task=task
        )

    except Exception as e:
        logger.error(f"Error during brain processing: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Processing failed: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn

    # Get port from environment variable or use default
    port = int(os.getenv("AIMINER_PORT", "8080"))

    logger.info(f"Starting AIminer Brain API Server on port {port}")
    logger.info("Set AIMINER_MODEL_PATH environment variable to specify model location")
    logger.info("Example: export AIMINER_MODEL_PATH=/path/to/model.gguf")

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=port,
        log_level="info"
    )
