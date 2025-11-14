"""
Prompt template for AI brain processing
"""

def generate_prompt(brain_data: dict) -> str:
    """
    Generate a prompt from brain data for the AI model

    Args:
        brain_data: Dictionary containing brain.json data

    Returns:
        Formatted prompt string
    """
    rules = brain_data.get("rules", {})
    vision = brain_data.get("vision", {})
    memory = brain_data.get("memory", {})

    # Extract chat messages
    chat_messages = vision.get("chat", [])
    chat_text = "\n".join([
        f"[{msg.get('timestamp', 0)}] {msg.get('player', 'Unknown')}: {msg.get('message', '')}"
        for msg in chat_messages[-10:]  # Last 10 messages
    ])

    # Extract block vision data
    block_vision = vision.get("blockVision", {})
    bot_position = block_vision.get("botPosition", {})
    visible_blocks = block_vision.get("visibleBlocks", [])

    blocks_text = "\n".join([
        f"  - {block.get('blockType', 'unknown')} at ({block.get('position', {}).get('x', 0)}, "
        f"{block.get('position', {}).get('y', 0)}, {block.get('position', {}).get('z', 0)}) "
        f"distance: {block.get('distance', 0):.1f}"
        for block in visible_blocks[:20]  # First 20 blocks
    ])

    # Extract memory
    memory_text = "\n".join([
        f"  - {key}: {value}"
        for key, value in memory.items()
    ])

    prompt = f"""You are an AI controlling a Minecraft bot. Based on the current situation, decide what action to take next.

# RULES (Your core programming)
{rules.get('description', 'No rules defined')}

# CURRENT SITUATION

## Bot Position
X: {bot_position.get('x', 0)}, Y: {bot_position.get('y', 0)}, Z: {bot_position.get('z', 0)}
View Direction: Yaw {bot_position.get('viewDirection', {}).get('yaw', 0):.1f}, Pitch {bot_position.get('viewDirection', {}).get('pitch', 0):.1f}

## Recent Chat Messages
{chat_text if chat_text else "No recent chat messages"}

## Visible Blocks (within your view)
{blocks_text if blocks_text else "No visible blocks detected"}

## Memory (Important information you've learned)
{memory_text if memory_text else "No memories stored"}

# YOUR TASK
Based on the above information, decide what to do next. You can respond with ONE of the following actions:

1. MINE_WOOD - Mine wood at a specific location
   Format: MINE_WOOD x y z

2. MINE_STONE - Mine stone at a specific location
   Format: MINE_STONE x y z

3. MOVE_TO - Move to a specific location
   Format: MOVE_TO x y z

4. CHAT - Send a chat message
   Format: CHAT <message>

5. GET_INVENTORY - Check your current inventory
   Format: GET_INVENTORY

6. GET_POSITION - Get your exact position
   Format: GET_POSITION

7. WAIT - Do nothing and wait
   Format: WAIT

Respond with ONLY the action command in the format shown above. Do not add explanations.

Your response:"""

    return prompt


def parse_ai_response(response: str) -> dict:
    """
    Parse AI model's response into a task object

    Args:
        response: Raw response from AI model

    Returns:
        Task dictionary or None if parsing fails
    """
    response = response.strip().upper()
    parts = response.split(None, 1)

    if not parts:
        return None

    action = parts[0]

    # Task template
    task = {
        "id": 0,  # Will be set by BrainFileManager
        "type": action,
        "status": "PENDING",
        "createdAt": 0,  # Will be set by caller
        "parameters": {}
    }

    # Parse parameters based on action type
    if action in ["MINE_WOOD", "MINE_STONE", "MOVE_TO"]:
        if len(parts) > 1:
            coords = parts[1].split()
            if len(coords) >= 3:
                try:
                    task["parameters"] = {
                        "x": int(coords[0]),
                        "y": int(coords[1]),
                        "z": int(coords[2])
                    }
                except ValueError:
                    return None
            else:
                return None
        else:
            return None

    elif action == "CHAT":
        if len(parts) > 1:
            task["parameters"] = {"message": parts[1]}
        else:
            return None

    elif action in ["GET_INVENTORY", "GET_POSITION", "WAIT"]:
        # No parameters needed
        pass

    else:
        # Unknown action
        return None

    return task
