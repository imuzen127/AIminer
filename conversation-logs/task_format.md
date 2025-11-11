# タスク情報フォーマット定義

## タスクの種類

### 1. コマンド系タスク

#### MINE_WOOD (木を掘る)
```json
{
  "id": 1,
  "type": "MINE_WOOD",
  "parameters": {
    "x": -13,
    "y": -55,
    "z": 47
  },
  "reason": "木を採掘する",
  "status": "pending"
}
```
実行コマンド: `/function imuzen127x74:xoak {x:-13,y:-55,z:47}`

#### MINE_STONE (石を掘る)
```json
{
  "id": 2,
  "type": "MINE_STONE",
  "parameters": {
    "x": -3,
    "y": -60,
    "z": 46
  },
  "reason": "石を採掘する",
  "status": "pending"
}
```
実行コマンド: `/function imuzen127x74:xstone {x:-3,y:-60,z:46}`

#### MOVE_TO (移動)
```json
{
  "id": 3,
  "type": "MOVE_TO",
  "parameters": {
    "x": -3,
    "y": -60,
    "z": 48
  },
  "reason": "指定座標へ移動",
  "status": "pending"
}
```
実行コマンド: `/function imuzen127x74:xaim {x:-3,y:-60,z:48}`

---

### 2. データ取得系タスク

#### GET_INVENTORY (インベントリ取得)
```json
{
  "id": 4,
  "type": "GET_INVENTORY",
  "parameters": {},
  "reason": "所持品を確認",
  "status": "pending"
}
```
実行コマンド: `/data get entity @e[tag=test1,limit=1] data.Inventory`
結果: memoryセクションの `inventory_state` に保存

#### GET_POSITION (自分の座標取得)
```json
{
  "id": 5,
  "type": "GET_POSITION",
  "parameters": {},
  "reason": "現在位置を確認",
  "status": "pending"
}
```
実行コマンド: `/data get entity @e[tag=test1,limit=1] Pos`
結果: memoryセクションの `current_position` に保存

#### GET_ENTITY_POSITION (他エンティティの座標取得)
```json
{
  "id": 6,
  "type": "GET_ENTITY_POSITION",
  "parameters": {
    "entity_selector": "@p[name=Player1]"
  },
  "reason": "Player1の位置を確認",
  "status": "pending"
}
```
実行コマンド: `/data get entity @p[name=Player1] Pos`
結果: memoryセクションに追加

#### CHAT (チャット発言)
```json
{
  "id": 7,
  "type": "CHAT",
  "parameters": {
    "message": "了解しました"
  },
  "reason": "プレイヤーに返事",
  "status": "pending"
}
```
実行コマンド: `/say 了解しました`

#### READ_MEMORY (記憶情報読み込み)
```json
{
  "id": 8,
  "type": "READ_MEMORY",
  "parameters": {
    "key": "important_locations"
  },
  "reason": "重要な場所を思い出す",
  "status": "pending"
}
```
内部処理: memoryセクションから指定キーのデータを読み込む

---

## タスクステータス

- `pending`: 実行待ち
- `in_progress`: 実行中
- `completed`: 完了
- `failed`: 失敗

---

## タスク実行フロー

1. tasksリストの先頭からタスクを取得
2. statusが`pending`のタスクを実行
3. 実行中は`in_progress`に変更
4. 完了したら`completed`に変更
5. 次のタスクへ進む

---

## タスク実行時の注意点

### コマンド系タスク
- データパックの function を呼び出す
- 実行には時間がかかる（瞬時ではない）
- 完了を待ってから次のタスクへ

### データ取得系タスク
- コマンドを実行して結果を取得
- 結果をmemoryセクションに保存
- 次の脳ファイル更新時にAIが参照可能

---

## Java実装での型定義

```java
public enum TaskType {
    MINE_WOOD,
    MINE_STONE,
    MOVE_TO,
    GET_INVENTORY,
    GET_POSITION,
    GET_ENTITY_POSITION,
    CHAT,
    READ_MEMORY
}

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

public class Task {
    private int id;
    private TaskType type;
    private Map<String, Object> parameters;
    private String reason;
    private TaskStatus status;

    // Getters and Setters
}
```
