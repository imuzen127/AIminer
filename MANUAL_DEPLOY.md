# AIminer - Manual Deployment Guide

手動でサーバーにファイルを配置する手順。

## 必要なファイルと配置場所

### 1. プラグイン (Minecraft Server)

**ファイル:**
```
plugin/AIminer/build/libs/AIminer-1.6-SNAPSHOT.jar
```

**配置先:**
```
[Minecraftサーバー]/plugins/AIminer-1.6-SNAPSHOT.jar
```

**サイズ:** 約42KB

---

### 2. データパック (Minecraft Server)

**フォルダ:**
```
datapack/manekinwalk_datapack/
```

**配置先:**
```
[Minecraftサーバー]/world/datapacks/manekinwalk_datapack/
```

**注意:** フォルダごとコピーしてください

---

### 3. AIセットアップファイル (任意の場所)

**フォルダ:**
```
ai-brain/
```

**配置先:**
```
[任意の場所]/ai-brain/
```

例:
- `/home/minecraft/ai-brain/`
- `/opt/aiminer/ai-brain/`
- Minecraftサーバーと同じディレクトリでもOK

**必要なファイル一覧:**
```
ai-brain/
├── setup.sh          ← これを実行
├── setup.bat         (Windows用)
├── start_server.sh
├── start_server.bat
├── api_server.py
├── prompt_template.py
├── brain_processor.py
├── requirements.txt
└── README.md
```

---

## 配置後の手順

### Step 1: AIサーバーをセットアップ

```bash
cd ai-brain
chmod +x setup.sh
./setup.sh
```

**対話形式で聞かれること:**
- Hugging Face モデルリポジトリ名（例: `imuzen127/your-model`）

**setup.shが自動的に行うこと:**
- ✓ Python依存関係のインストール
- ✓ Hugging Faceからモデルダウンロード
- ✓ 環境設定ファイル（.env）作成
- ✓ 起動スクリプト（start.sh）生成

### Step 2: AIサーバーを起動

```bash
cd ai-brain
./start.sh
```

**確認:**
```
Model loaded successfully in 15.23 seconds!
INFO:     Uvicorn running on http://0.0.0.0:8080
```

### Step 3: Minecraftサーバーを起動

通常通りMinecraftサーバーを起動

**ログ確認:**
```
[AIminer] AIminer plugin has been enabled!
[AIminer] AI processing system started (server: http://localhost:8080)
[AIminer] Vision update system started.
```

### Step 4: ゲーム内でテスト

1. サーバーに接続
2. コマンド実行: `/bot start`
3. 10秒待つ

**サーバーログ確認:**
```
[AIminer] Starting AI brain processing...
[AIminer] AI processing completed in 1234ms - Task added: true
[AIminer] New task generated: MINE_WOOD (ID: 1)
```

---

## ファイル配置例

### パターン1: サーバーと同じディレクトリ

```
/home/minecraft/
├── server.jar
├── plugins/
│   └── AIminer-1.6-SNAPSHOT.jar
├── world/
│   └── datapacks/
│       └── manekinwalk_datapack/
└── ai-brain/           ← ここに配置
    ├── setup.sh
    ├── api_server.py
    └── ...
```

### パターン2: 別の場所

```
/home/minecraft/
├── server.jar
├── plugins/
│   └── AIminer-1.6-SNAPSHOT.jar
└── world/
    └── datapacks/
        └── manekinwalk_datapack/

/opt/aiminer/
└── ai-brain/           ← ここに配置
    ├── setup.sh
    ├── api_server.py
    └── ...
```

どちらでもOK。`config.yml`でAIサーバーのURLを指定できます。

---

## 設定ファイル（必要に応じて編集）

### プラグイン設定

**場所:**
```
[Minecraftサーバー]/plugins/AIminer/config.yml
```

**内容:**
```yaml
ai-server:
  url: "http://localhost:8080"  # AIサーバーのURL
  enabled: true
  interval: 10                  # AI処理間隔（秒）

vision:
  scan-radius: 10               # ブロックスキャン半径
  update-interval: 5            # 視覚更新間隔（秒）
```

**編集後:** Minecraftサーバーを再起動

### AIサーバー設定

**場所:**
```
ai-brain/.env
```

**内容:**
```
AIMINER_MODEL_PATH=./models/model-name.gguf
AIMINER_PORT=8080
```

**編集後:** AIサーバーを再起動（`./start.sh`）

---

## トラブルシューティング

### プラグインが読み込まれない

**確認:**
```bash
ls -lh [サーバー]/plugins/AIminer-1.6-SNAPSHOT.jar
```

ファイルが存在し、サイズが約42KBであることを確認。

### データパックが認識されない

**確認:**
```bash
ls -lh [サーバー]/world/datapacks/manekinwalk_datapack/
```

フォルダ構造が正しいか確認。

**ゲーム内で確認:**
```
/datapack list
```

`manekinwalk_datapack` が表示されるはず。

### AIサーバーに接続できない

**AIサーバーが起動しているか確認:**
```bash
curl http://localhost:8080/health
```

**応答例:**
```json
{
  "status": "healthy",
  "model_loaded": true
}
```

**起動していない場合:**
```bash
cd ai-brain
./start.sh
```

### モデルが見つからない

**モデルファイルを確認:**
```bash
ls -lh ai-brain/models/*.gguf
```

**ない場合は手動ダウンロード:**
```bash
cd ai-brain
python3 -m huggingface_hub.commands.download_cli \
    --repo-id YOUR_REPO \
    --local-dir models
```

---

## 診断ツール

問題が発生した場合、診断スクリプトを実行：

```bash
cd AIminer
chmod +x diagnostics.sh
./diagnostics.sh
```

すべてのコンポーネントをチェックして、問題箇所を特定します。

---

## ファイル取得方法

### GitHubから直接ダウンロード

```bash
# サーバー上で実行
git clone https://github.com/imuzen127/AIminer.git
```

### ビルド済みファイルを使用

ローカルでビルドしてからアップロード：

```bash
# ローカルPC上で
cd AIminer/plugin/AIminer
./gradlew build

# build/libs/AIminer-1.6-SNAPSHOT.jar が生成される
```

このJARをサーバーにアップロード。

---

## 最小限の配置（テスト用）

最小限で動作確認したい場合：

1. **プラグインのみ配置** → サーバー起動 → エラー確認
2. **データパック追加** → サーバー再起動 → `/bot start` でエラー確認
3. **AIサーバー起動** → 完全動作確認

段階的にテストできます。

---

## まとめ

**配置するもの:**
1. `AIminer-1.6-SNAPSHOT.jar` → `plugins/`
2. `manekinwalk_datapack/` → `world/datapacks/`
3. `ai-brain/` → 任意の場所

**実行すること:**
1. `cd ai-brain && ./setup.sh`
2. `./start.sh` でAIサーバー起動
3. Minecraftサーバー起動
4. `/bot start` でテスト

これだけです！
