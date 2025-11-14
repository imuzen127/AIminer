# AIminer プラグイン 使用ガイド

## 概要

AIminerは、MinecraftのPaper 1.21サーバー向けプラグインで、AIボットの「脳」を管理します。
プレイヤーのチャット、ボットの視覚情報、メモリを記録し、AIと連携してボットが自律的に行動します。

---

## ビルド方法

### 前提条件
- Java 21以上
- Gradle（Gradle Wrapperが含まれているため、別途インストール不要）

### ビルド手順

1. プラグインディレクトリに移動：
   ```bash
   cd plugin/AIminer
   ```

2. Gradleでビルド：
   ```bash
   ./gradlew build
   ```
   Windowsの場合：
   ```cmd
   gradlew.bat build
   ```

3. ビルド成功後、JARファイルが生成されます：
   ```
   build/libs/AIminer-1.0-SNAPSHOT.jar
   ```

### クリーンビルド

前回のビルドをクリーンしてから再ビルドする場合：
```bash
./gradlew clean build
```

---

## サーバーへの導入

### 前提条件
- Paper 1.21サーバーがインストールされている
- サーバーが停止している状態

### 導入手順

1. **JARファイルをコピー**
   ```bash
   # ビルドしたJARファイルをサーバーのpluginsフォルダにコピー
   cp build/libs/AIminer-1.0-SNAPSHOT.jar /path/to/server/plugins/
   ```

   Windowsの場合：
   ```cmd
   copy build\libs\AIminer-1.0-SNAPSHOT.jar C:\path\to\server\plugins\
   ```

2. **サーバーを起動**
   ```bash
   cd /path/to/server
   java -jar paper-1.21.jar
   ```

3. **プラグインの読み込み確認**

   サーバーログに以下のようなメッセージが表示されれば成功：
   ```
   [Server] INFO Enabling AIminer v1.0-SNAPSHOT
   ```

4. **brain.jsonの生成確認**

   プラグインが起動すると、以下の場所にbrain.jsonが自動生成されます：
   ```
   plugins/AIminer/brain.json
   ```

---

## 現在実装されている機能

### 1. 脳ファイル管理（brain.json）

プラグインは起動時に `plugins/AIminer/brain.json` を生成・読み込みます。

**脳ファイルの構造**：
```json
{
  "rules": {
    "description": "AIの行動ルール"
  },
  "vision": {
    "chat": [
      {
        "player": "プレイヤー名",
        "message": "メッセージ内容",
        "timestamp": 1699999999000
      }
    ],
    "blockVision": {
      "botPosition": {
        "x": 0,
        "y": 64,
        "z": 0,
        "viewDirection": {
          "yaw": 0.0,
          "pitch": 0.0
        }
      },
      "visibleBlocks": []
    }
  },
  "memory": {},
  "tasks": []
}
```

### 2. チャット監視

プレイヤーがチャットを送信すると、自動的に `brain.json` の `vision.chat` に記録されます。

**記録される情報**：
- プレイヤー名
- メッセージ内容
- タイムスタンプ

**履歴の保持数**：
- 最新20件のチャットメッセージのみ保持
- 古いメッセージは自動的に削除されます

### 3. プラグインのライフサイクル管理

- **起動時**: brain.jsonを読み込み（存在しない場合は新規作成）
- **終了時**: brain.jsonを自動保存

---

## 使用例

### 基本的な使い方

1. **サーバーを起動**
   ```bash
   java -jar paper-1.21.jar
   ```

2. **Minecraftに接続**

   サーバーに接続すると、プラグインがチャットを監視開始します。

3. **チャットを送信**
   ```
   /say こんにちは、ボット！
   ```

4. **brain.jsonを確認**
   ```bash
   cat plugins/AIminer/brain.json
   ```

   チャットメッセージが記録されているはずです：
   ```json
   {
     "vision": {
       "chat": [
         {
           "player": "YourName",
           "message": "こんにちは、ボット！",
           "timestamp": 1731567890000
         }
       ]
     }
   }
   ```

---

## データパックとの連携

AIminerプラグインは、データパック側のボット制御機能と連携します。

### データパック側の主要コマンド

#### ボット召喚
```mcfunction
# 見た目召喚
/function imuzen127x74:summanekin

# 足召喚
/function imuzen127x74:sumpig
```

#### ボット操作
```mcfunction
# 木を掘る（常時実行）
/function imuzen127x74:xoak {x:-13,y:-55,z:47}

# 石を掘る（常時実行）
/function imuzen127x74:xstone {x:-3,y:-60,z:46}

# 移動（一度だけ実行）
/function imuzen127x74:xaim {x:-3,y:-60,z:48}
```

#### ボットのデータ取得
```mcfunction
# ボットの全データ取得
/data get entity @e[tag=test1,limit=1] data

# インベントリ取得
/data get entity @e[tag=test1,limit=1] data.Inventory
```

---

## AI連携（GitHub Actions）

プラグインが生成した `brain.json` をGitHubにプッシュすると、GitHub Actionsが自動的にAI推論を実行します。

### 連携フロー
```
1. プラグインがbrain.jsonを生成・更新
   ↓
2. brain.jsonをGitHubにプッシュ
   ↓
3. GitHub Actions自動起動
   ↓
4. Hugging Faceからモデルダウンロード
   ↓
5. AI推論実行（次のタスクを決定）
   ↓
6. brain.jsonのtasksセクションを更新
   ↓
7. GitHubに自動コミット・プッシュ
   ↓
8. プラグインが更新されたbrain.jsonを取得
   ↓
9. タスクを実行（今後実装予定）
```

### GitHub Actionsの手動実行

1. https://github.com/YOUR_USERNAME/AIminer/actions にアクセス
2. "AI Brain Processing" ワークフローを選択
3. "Run workflow" をクリック
4. "Run workflow" ボタンをクリック

---

## 設定ファイル

### プラグイン設定：paper-plugin.yml

プラグインのメタデータは `src/main/resources/paper-plugin.yml` で定義されています：

```yaml
name: AIminer
version: '${version}'
main: plugin.midorin.info.aIminer.AIminer
api-version: '1.21'
```

### Gradle設定：build.gradle

依存関係やビルド設定は `build.gradle` で管理されています：

```gradle
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.11.0")
}
```

---

## トラブルシューティング

### 問題1: プラグインが読み込まれない

**症状**：
サーバーログに "Enabling AIminer" が表示されない

**原因**：
- JARファイルが正しいpluginsフォルダにない
- Paperサーバーのバージョンが1.21でない
- Java 21以上がインストールされていない

**解決策**：
1. pluginsフォルダのパスを確認
2. サーバーのバージョンを確認：`version` コマンド
3. Javaのバージョンを確認：`java -version`

### 問題2: brain.jsonが生成されない

**症状**：
`plugins/AIminer/brain.json` が存在しない

**原因**：
- プラグインが正しく起動していない
- ファイル書き込み権限がない

**解決策**：
1. サーバーログでエラーメッセージを確認
2. pluginsフォルダの書き込み権限を確認
3. プラグインを再起動

### 問題3: チャットが記録されない

**症状**：
チャットを送信してもbrain.jsonに記録されない

**原因**：
- ChatListenerが正しく登録されていない
- brain.jsonが壊れている

**解決策**：
1. サーバーを再起動
2. brain.jsonを削除して自動再生成させる
3. サーバーログでエラーを確認

### 問題4: ビルドが失敗する

**症状**：
`./gradlew build` がエラーで終了する

**よくあるエラーと解決策**：

**エラー1**: `Java version mismatch`
```
解決策: Java 21以上がインストールされているか確認
java -version
```

**エラー2**: `Dependency resolution failed`
```
解決策: インターネット接続を確認し、再度ビルド
./gradlew build --refresh-dependencies
```

**エラー3**: `Permission denied`
```
解決策: gradlewに実行権限を付与
chmod +x gradlew
```

---

## 開発情報

### プロジェクト構造
```
plugin/AIminer/
├── src/
│   └── main/
│       ├── java/
│       │   └── plugin/midorin/info/aIminer/
│       │       ├── AIminer.java              # メインクラス
│       │       ├── brain/
│       │       │   └── BrainFileManager.java # 脳ファイル管理
│       │       ├── listener/
│       │       │   └── ChatListener.java     # チャットイベント監視
│       │       └── model/                    # データモデル（11クラス）
│       └── resources/
│           └── paper-plugin.yml              # プラグイン定義
├── build.gradle                              # Gradle設定
├── gradlew                                   # Gradle Wrapper (Unix)
├── gradlew.bat                               # Gradle Wrapper (Windows)
└── PLUGIN_GUIDE.md                           # このファイル
```

### 実装済みクラス

**データモデル（model/）**：
- `TaskType` - タスクの種類
- `TaskStatus` - タスクのステータス
- `Task` - タスク情報
- `ChatMessage` - チャットメッセージ
- `Position` - 3D座標
- `ViewDirection` - 視線方向
- `VisibleBlock` - 視界内のブロック
- `BlockVisionData` - ブロック視覚データ
- `VisionData` - 視覚情報統合
- `Memory` - 記憶情報
- `BrainRules` - 約束ごと
- `BrainData` - 脳ファイル全体

**コアシステム**：
- `BrainFileManager` - 脳ファイルの読み書き、チャット管理
- `ChatListener` - チャットイベント監視
- `AIminer` - メインプラグインクラス

### 未実装の機能

以下の機能は今後実装予定です：

1. **視覚情報の取得**
   - ボット周囲のブロックスキャン
   - レイキャスト方式での視界判定

2. **タスク実行システム**
   - brain.jsonのtasksセクションを監視
   - データパックコマンドの自動実行

3. **GitHub同期機能**
   - brain.jsonの自動プッシュ
   - 更新されたbrain.jsonの自動取得

---

## 関連ドキュメント

- [開発セッション記録（2025-11-11）](../../conversation-logs/session_2025-11-11.md)
- [開発セッション記録（2025-11-14）](../../conversation-logs/session_2025-11-14.md)
- [脳ファイル仕様](../../conversation-logs/brain_file_spec.md)
- [視覚システム設計](../../conversation-logs/vision_system_design.md)
- [タスクフォーマット](../../conversation-logs/task_format.md)
- [Hugging Faceアップロードガイド](../../conversation-logs/huggingface_upload_guide.md)

---

## ライセンス

(未定)

---

## 作者

imuzen127

---

## サポート

問題が発生した場合は、GitHubリポジトリのIssuesで報告してください：
https://github.com/imuzen127/AIminer/issues
