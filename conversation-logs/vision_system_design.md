# 視覚情報取得システム設計

## 概要

ボットが「見る」ことができる情報を取得するシステム。
プレイヤーと同じように視界に基づいてブロック情報を取得し、透視は不可。

---

## 1. チャット情報取得

### 実装方法
- Bukkit の `AsyncPlayerChatEvent` をリスナーで取得
- チャット履歴を保持（最新20件程度）
- タイムスタンプ、プレイヤー名、メッセージを記録

### クラス設計
```java
public class ChatListener implements Listener {
    private final BrainFileManager brainManager;

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        long timestamp = System.currentTimeMillis();

        brainManager.addChatMessage(player, message, timestamp);
    }
}
```

---

## 2. ブロック情報取得（視界ベース）

### 要件
- ボットの視界に基づいてブロックを取得
- 手前のブロックで遮られる場合、その先は見えない
- プレイヤーの見ている範囲と同等

### パラメータ
- **視野距離**: 10ブロック（設定可能）
- **視野角（FOV）**: 90度（プレイヤーと同等）
- **視野の高さ**: ±45度（上下方向）

### アルゴリズム

#### 2.1 基本情報の取得
1. ボットエンティティ（`@e[tag=test1]`）の位置を取得
2. ボットの向き（yaw, pitch）を取得
3. 視線の開始位置を計算（エンティティの目の高さ）

#### 2.2 視野範囲の計算
```
視野角: 90度（左右各45度）
視野の高さ: 90度（上下各45度）
視野距離: 10ブロック
```

#### 2.3 レイキャスト方式

**方法1: グリッドスキャン（推奨）**
```
1. 視野角を格子状に分割（例: 5度刻み）
2. 各方向にレイキャストを実行
3. 最初にヒットしたブロック（AIR以外）を記録
4. 遮蔽物があればその先はスキャンしない
```

**方法2: ブロック直接スキャン**
```
1. ボット周囲の立方体範囲（10x10x10）を取得
2. 各ブロックに対して視線が通るかチェック
3. 手前のブロックで遮られていないか判定
4. 視野角内にあるかチェック
```

#### 2.4 遮蔽判定

```java
public boolean isBlockVisible(Location botLocation, Location blockLocation, World world) {
    // 視線ベクトルを計算
    Vector direction = blockLocation.toVector().subtract(botLocation.toVector()).normalize();

    // レイキャストで間に遮蔽物がないか確認
    RayTraceResult result = world.rayTraceBlocks(
        botLocation,
        direction,
        botLocation.distance(blockLocation),
        FluidCollisionMode.NEVER,
        true
    );

    // 遮蔽物がある場合は見えない
    if (result != null && result.getHitBlock() != null) {
        Block hitBlock = result.getHitBlock();
        // ヒットしたブロックが目標ブロックと同じなら見える
        return hitBlock.equals(blockLocation.getBlock());
    }

    return true;
}
```

### データ構造

```java
public class VisibleBlock {
    private Vector relativePosition;  // ボットからの相対座標
    private Location worldPosition;   // ワールド座標
    private Material blockType;       // ブロックタイプ
    private double distance;          // ボットからの距離

    // Getters and Setters
}

public class VisionData {
    private List<ChatMessage> chatHistory;
    private BlockVisionData blockVision;
}

public class BlockVisionData {
    private int viewDistance;
    private double yaw;
    private double pitch;
    private List<VisibleBlock> visibleBlocks;
}
```

---

## 3. 視覚情報の更新頻度

### チャット情報
- イベント駆動（チャットが発生したら即座に更新）
- 履歴は最新20件を保持

### ブロック情報
- **オプション1**: 1秒ごとに更新（推奨）
  - サーバー負荷を考慮
  - ボットの動きに十分追従可能

- **オプション2**: Tickごとに更新（20回/秒）
  - リアルタイム性が高い
  - サーバー負荷が高い

### 実装
```java
public class VisionUpdateTask extends BukkitRunnable {
    private final BotVisionSystem visionSystem;
    private final Entity botEntity;

    @Override
    public void run() {
        // ブロック情報を更新
        BlockVisionData blockData = visionSystem.scanVisibleBlocks(botEntity);
        brainManager.updateBlockVision(blockData);
    }
}

// プラグイン初期化時
visionUpdateTask.runTaskTimer(plugin, 0L, 20L); // 1秒ごと
```

---

## 4. パフォーマンス最適化

### ブロックスキャンの最適化
1. **視野角外のブロックをスキップ**
   - ボットの向きから±45度以内のみスキャン

2. **距離による間引き**
   - 近距離: 1ブロック刻み
   - 遠距離: 2ブロック刻み

3. **AIRブロックの除外**
   - AIRブロックは記録しない（必要な場合のみ記録）

4. **非同期処理**
   - ブロックスキャンは非同期タスクで実行
   - メインスレッドをブロックしない

```java
public class VisionUpdateTask extends BukkitRunnable {
    @Override
    public void run() {
        // 非同期で視覚情報を取得
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BlockVisionData data = scanBlocks();

            // メインスレッドで脳ファイルを更新
            Bukkit.getScheduler().runTask(plugin, () -> {
                brainManager.updateBlockVision(data);
            });
        });
    }
}
```

---

## 5. クラス構成

```
plugin.midorin.info.aIminer
├── AIminer.java                    # メインプラグインクラス
├── brain/
│   ├── BrainFileManager.java      # 脳ファイル管理
│   ├── BrainData.java              # 脳データモデル
│   └── TaskExecutor.java           # タスク実行
├── vision/
│   ├── BotVisionSystem.java       # 視覚情報統合管理
│   ├── BlockScanner.java           # ブロックスキャン
│   ├── ChatListener.java           # チャット監視
│   ├── VisionUpdateTask.java      # 定期更新タスク
│   └── model/
│       ├── VisibleBlock.java       # ブロック情報モデル
│       ├── ChatMessage.java        # チャットメッセージモデル
│       ├── VisionData.java         # 視覚情報統合モデル
│       └── BlockVisionData.java    # ブロック視覚データ
└── util/
    └── VectorUtil.java             # ベクトル計算ユーティリティ
```

---

## 6. 実装の優先順位

1. **Phase 1**: チャット情報取得（簡単）
   - ChatListener の実装
   - 脳ファイルへの書き込み

2. **Phase 2**: 基本的なブロック取得（簡易版）
   - ボット周囲の立方体範囲を取得
   - 視野角や遮蔽は考慮しない

3. **Phase 3**: 視野角の実装
   - ボットの向きを考慮
   - 視野角内のブロックのみ取得

4. **Phase 4**: 遮蔽判定の実装
   - レイキャストで遮蔽判定
   - 見えないブロックを除外

5. **Phase 5**: パフォーマンス最適化
   - 非同期処理
   - スキャン範囲の最適化
