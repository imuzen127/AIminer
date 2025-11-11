# 脳ファイル仕様書

## 概要
ボットの「脳」を表現するJSON形式のファイル。AIがこのファイルを読み取り、書き換えることで行動を決定する。

## ファイル構造

脳ファイルは以下の4つのセクションで構成される：

1. **rules** (約束ごと) - AI本能部分
2. **vision** (視覚情報) - チャットとブロック情報
3. **memory** (記憶情報) - 重要な情報の記録
4. **tasks** (タスク情報) - 実行する行動

---

## 1. rules (約束ごと)

AIの本能部分。脳ファイルの読み方と書き換え方のルールを記述。

```json
{
  "rules": {
    "description": "このファイルはボットの脳です。visionから情報を読み取り、必要な情報をmemoryに追加し、tasksに実行したい行動を記述してください。",
    "vision_rules": "visionは常に更新されます。重要な情報はmemoryに保存してください。",
    "memory_rules": "memoryには重要な情報のみを記録してください。不要になった情報は削除可能です。",
    "task_rules": "tasksには次に実行したい行動を記述してください。タスクは順番に実行されます。",
    "available_tasks": [
      {
        "type": "MINE_WOOD",
        "description": "木を掘る",
        "parameters": ["x", "y", "z"],
        "command": "/function imuzen127x74:xoak {x:X,y:Y,z:Z}"
      },
      {
        "type": "MINE_STONE",
        "description": "石を掘る",
        "parameters": ["x", "y", "z"],
        "command": "/function imuzen127x74:xstone {x:X,y:Y,z:Z}"
      },
      {
        "type": "MOVE_TO",
        "description": "特定の場所へ移動",
        "parameters": ["x", "y", "z"],
        "command": "/function imuzen127x74:xaim {x:X,y:Y,z:Z}"
      },
      {
        "type": "GET_INVENTORY",
        "description": "インベントリ情報を取得",
        "parameters": [],
        "command": "/data get entity @e[tag=test1,limit=1] data.Inventory"
      },
      {
        "type": "GET_POSITION",
        "description": "自分の座標を取得",
        "parameters": [],
        "command": "/data get entity @e[tag=test1,limit=1] Pos"
      },
      {
        "type": "GET_ENTITY_POSITION",
        "description": "他のエンティティの座標を取得",
        "parameters": ["entity_selector"],
        "command": "/data get entity <selector> Pos"
      },
      {
        "type": "CHAT",
        "description": "チャットで発言",
        "parameters": ["message"],
        "command": "/say <message>"
      },
      {
        "type": "READ_MEMORY",
        "description": "記憶情報を読み込む",
        "parameters": ["key"],
        "command": "internal"
      }
    ]
  }
}
```

---

## 2. vision (視覚情報)

常に更新される情報。チャットとブロック情報を含む。

### 2.1 チャット情報

```json
{
  "vision": {
    "chat": [
      {
        "timestamp": "2025-11-11T10:30:00",
        "player": "Player1",
        "message": "木を取ってきて"
      },
      {
        "timestamp": "2025-11-11T10:31:00",
        "player": "Player2",
        "message": "こんにちは"
      }
    ]
  }
}
```

### 2.2 ブロック情報

ボットの視界に基づいて取得。手前のブロックで遮られる場合、その先は見えない。

```json
{
  "vision": {
    "blocks": {
      "view_distance": 10,
      "view_direction": {
        "yaw": 90.0,
        "pitch": 0.0
      },
      "visible_blocks": [
        {
          "relative_position": {"x": 1, "y": 0, "z": 0},
          "world_position": {"x": -13, "y": -55, "z": 47},
          "block_type": "minecraft:oak_log",
          "distance": 1.0
        },
        {
          "relative_position": {"x": 2, "y": 0, "z": 0},
          "world_position": {"x": -12, "y": -55, "z": 47},
          "block_type": "minecraft:oak_log",
          "distance": 2.0
        },
        {
          "relative_position": {"x": 0, "y": 1, "z": 0},
          "world_position": {"x": -14, "y": -54, "z": 47},
          "block_type": "minecraft:air",
          "distance": 1.0
        }
      ]
    }
  }
}
```

---

## 3. memory (記憶情報)

視覚情報から選別された重要な情報を保存。

```json
{
  "memory": {
    "important_locations": [
      {
        "name": "木の場所",
        "position": {"x": -13, "y": -55, "z": 47},
        "note": "Player1が木を取ってきてと言った場所"
      }
    ],
    "player_requests": [
      {
        "player": "Player1",
        "request": "木を取ってきて",
        "status": "pending",
        "timestamp": "2025-11-11T10:30:00"
      }
    ],
    "inventory_state": {
      "last_checked": "2025-11-11T10:25:00",
      "items": [
        {"type": "minecraft:oak_log", "count": 5}
      ]
    },
    "current_position": {
      "x": -14,
      "y": -55,
      "z": 47
    }
  }
}
```

---

## 4. tasks (タスク情報)

次に実行したい行動のリスト。順番に実行される。

```json
{
  "tasks": [
    {
      "id": 1,
      "type": "MOVE_TO",
      "parameters": {
        "x": -13,
        "y": -55,
        "z": 47
      },
      "reason": "木がある場所へ移動する",
      "status": "pending"
    },
    {
      "id": 2,
      "type": "MINE_WOOD",
      "parameters": {
        "x": -13,
        "y": -55,
        "z": 47
      },
      "reason": "Player1の依頼で木を採掘",
      "status": "pending"
    },
    {
      "id": 3,
      "type": "GET_INVENTORY",
      "parameters": {},
      "reason": "採掘後のインベントリを確認",
      "status": "pending"
    }
  ]
}
```

---

## 完全なサンプルファイル

```json
{
  "rules": {
    "description": "このファイルはボットの脳です。visionから情報を読み取り、必要な情報をmemoryに追加し、tasksに実行したい行動を記述してください。",
    "vision_rules": "visionは常に更新されます。重要な情報はmemoryに保存してください。",
    "memory_rules": "memoryには重要な情報のみを記録してください。不要になった情報は削除可能です。",
    "task_rules": "tasksには次に実行したい行動を記述してください。タスクは順番に実行されます。",
    "available_tasks": ["MINE_WOOD", "MINE_STONE", "MOVE_TO", "GET_INVENTORY", "GET_POSITION", "GET_ENTITY_POSITION", "CHAT", "READ_MEMORY"]
  },
  "vision": {
    "chat": [
      {
        "timestamp": "2025-11-11T10:30:00",
        "player": "Player1",
        "message": "木を取ってきて"
      }
    ],
    "blocks": {
      "view_distance": 10,
      "view_direction": {"yaw": 90.0, "pitch": 0.0},
      "visible_blocks": [
        {
          "relative_position": {"x": 1, "y": 0, "z": 0},
          "world_position": {"x": -13, "y": -55, "z": 47},
          "block_type": "minecraft:oak_log",
          "distance": 1.0
        }
      ]
    }
  },
  "memory": {
    "important_locations": [],
    "player_requests": [],
    "inventory_state": {},
    "current_position": {"x": -14, "y": -55, "z": 47}
  },
  "tasks": []
}
```

---

## プラグイン側の処理フロー

1. **視覚情報の更新** (毎tick or 一定間隔)
   - チャットイベントをリスナーで取得
   - ボットの視界内のブロック情報をレイキャストで取得
   - `vision`セクションを更新

2. **脳ファイルの送信** (一定間隔)
   - 更新された脳ファイルをAIサーバーに送信

3. **AIからの応答受信**
   - AIが更新した脳ファイルを受信
   - `tasks`セクションを解析

4. **タスクの実行**
   - `tasks`の先頭から順に実行
   - コマンド系タスク → Bukkitコマンド実行
   - データ取得系タスク → データ取得後`memory`に追加

5. **実行結果の反映**
   - 実行したタスクのstatusを"completed"に更新
   - 次の脳ファイル更新サイクルへ
