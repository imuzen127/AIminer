# AIminer AI Server - Quick Start

## 起動方法（サーバー側）

### Windows
```cmd
cd C:\Users\Administrator\Desktop\ai-brain
start.bat
```

### Linux/Mac
```bash
cd /path/to/ai-brain
./start.sh
```

---

## 起動確認

### 成功時のログ
```
Starting AIminer AI Server...
Model: models/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf
Port: 8080

INFO:AIminer-API:Loading AI model from: models/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf
INFO:AIminer-API:Model loaded successfully in 0.43 seconds!
INFO:AIminer-API:Server ready to process requests
INFO:     Uvicorn running on http://0.0.0.0:8080
```

### 健全性チェック
```cmd
curl http://localhost:8080/health
```

**期待される応答:**
```json
{
  "status": "healthy",
  "model_loaded": true
}
```

---

## 停止方法

**Ctrl + C** を押す

---

## トラブルシューティング

### エラー: "Model file not found"
```cmd
dir models\*.gguf
```
Mistral-7B-Instruct-v0.3-Q4_K_M.gguf があることを確認

### エラー: "Port 8080 already in use"
別のプロセスが8080を使用中
```cmd
# Windowsで確認
netstat -ano | findstr :8080

# Linuxで確認
lsof -i :8080
```

### エラー: "llama-cpp-python not found"
```cmd
pip install llama-cpp-python --prefer-binary
```

---

## 設定変更

### モデルを変更したい
**ファイル:** `ai-brain/.env`
```env
AIMINER_MODEL_PATH=models/別のモデル.gguf
AIMINER_PORT=8080
```

編集後、AIサーバーを再起動

---

## ローカル開発用（このPC）

```bash
cd C:/Users/imuze/AIminer/ai-brain
./start.sh
```

※ モデルファイルは別途ダウンロード必要

---

## 重要なパス（サーバー側）

```
AI Server: C:\Users\Administrator\Desktop\ai-brain\
モデル:     C:\Users\Administrator\Desktop\ai-brain\models\Mistral-7B-Instruct-v0.3-Q4_K_M.gguf
設定:       C:\Users\Administrator\Desktop\ai-brain\.env
起動:       C:\Users\Administrator\Desktop\ai-brain\start.bat
```

---

## 毎回の起動手順

1. **リモートデスクトップでサーバーに接続**
2. **コマンドプロンプトを開く**
3. **AIサーバー起動:**
   ```cmd
   cd C:\Users\Administrator\Desktop\ai-brain
   start.bat
   ```
4. **"Model loaded successfully" を確認**
5. **Minecraftサーバー起動**
6. **ゲーム内で `/bot start`**

---

以上！
