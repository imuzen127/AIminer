#!/usr/bin/env python3
"""
AI Brain Processor for AIminer Minecraft Bot
Processes brain.json using a GGUF language model
"""

import json
import sys
import time
from pathlib import Path
from typing import Optional

try:
    from llama_cpp import Llama
except ImportError:
    print("Error: llama-cpp-python is not installed.", file=sys.stderr)
    print("Install it with: pip install llama-cpp-python", file=sys.stderr)
    sys.exit(1)

from prompt_template import generate_prompt, parse_ai_response


class BrainProcessor:
    """Process brain.json using AI model"""

    def __init__(self, model_path: str, brain_path: str):
        """
        Initialize the brain processor

        Args:
            model_path: Path to the GGUF model file
            brain_path: Path to brain.json file
        """
        self.model_path = Path(model_path)
        self.brain_path = Path(brain_path)
        self.llm: Optional[Llama] = None

    def load_model(self):
        """Load the GGUF model"""
        print(f"Loading model from {self.model_path}...")
        if not self.model_path.exists():
            raise FileNotFoundError(f"Model file not found: {self.model_path}")

        # Load model with llama.cpp
        # n_ctx: context size, n_threads: CPU threads
        # n_gpu_layers: -1 for full GPU offload (if available)
        self.llm = Llama(
            model_path=str(self.model_path),
            n_ctx=4096,  # Context window
            n_threads=4,  # CPU threads
            n_gpu_layers=-1,  # Use GPU if available
            verbose=False
        )
        print("Model loaded successfully!")

    def load_brain(self) -> dict:
        """Load brain.json file"""
        print(f"Loading brain file from {self.brain_path}...")
        if not self.brain_path.exists():
            raise FileNotFoundError(f"Brain file not found: {self.brain_path}")

        with open(self.brain_path, 'r', encoding='utf-8') as f:
            brain_data = json.load(f)

        print("Brain file loaded successfully!")
        return brain_data

    def save_brain(self, brain_data: dict):
        """Save brain.json file"""
        print(f"Saving brain file to {self.brain_path}...")
        with open(self.brain_path, 'w', encoding='utf-8') as f:
            json.dump(brain_data, f, indent=2, ensure_ascii=False)
        print("Brain file saved successfully!")

    def process(self) -> bool:
        """
        Main processing function

        Returns:
            True if processing was successful
        """
        try:
            # Load model and brain
            self.load_model()
            brain_data = self.load_brain()

            # Generate prompt from brain data
            prompt = generate_prompt(brain_data)
            print("\n" + "="*60)
            print("GENERATED PROMPT:")
            print("="*60)
            print(prompt)
            print("="*60 + "\n")

            # Run inference
            print("Running AI inference...")
            response = self.llm(
                prompt,
                max_tokens=128,  # Max tokens to generate
                temperature=0.7,  # Creativity (0.0-1.0)
                top_p=0.9,       # Nucleus sampling
                stop=["\n"],     # Stop at newline
                echo=False       # Don't echo the prompt
            )

            # Extract text from response
            ai_output = response['choices'][0]['text'].strip()
            print(f"\nAI Response: {ai_output}\n")

            # Parse response into task
            task = parse_ai_response(ai_output)

            if task is None:
                print("Warning: Could not parse AI response into a valid task.", file=sys.stderr)
                return False

            # Add timestamp
            task['createdAt'] = int(time.time() * 1000)

            # Add task to brain data
            if 'tasks' not in brain_data:
                brain_data['tasks'] = []

            # Generate task ID
            existing_ids = [t.get('id', 0) for t in brain_data['tasks']]
            task['id'] = max(existing_ids, default=0) + 1

            brain_data['tasks'].append(task)

            print(f"Generated task: {task}")

            # Save updated brain
            self.save_brain(brain_data)

            return True

        except Exception as e:
            print(f"Error during processing: {e}", file=sys.stderr)
            import traceback
            traceback.print_exc()
            return False


def main():
    """Main entry point"""
    if len(sys.argv) < 3:
        print("Usage: python brain_processor.py <model_path> <brain_json_path>")
        print("Example: python brain_processor.py ./models/model.gguf ./brain.json")
        sys.exit(1)

    model_path = sys.argv[1]
    brain_path = sys.argv[2]

    processor = BrainProcessor(model_path, brain_path)
    success = processor.process()

    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
