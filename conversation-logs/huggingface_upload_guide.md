# Hugging Face モデルアップロードガイド

このガイドでは、12GBのGGUFモデルファイルをHugging Faceにアップロードし、GitHub Actionsから利用する手順を説明します。

---

## 前提条件

- Hugging Faceアカウント (https://huggingface.co/)
- モデルファイル: `gpt-oss-20b-MXFP4.gguf` (12GB)
- Python 3.8以上がインストールされていること

---

## ステップ1: Hugging Faceアカウントの作成とトークン取得

### 1.1 アカウント作成
https://huggingface.co/join にアクセスしてアカウントを作成します。

### 1.2 アクセストークンの作成
1. https://huggingface.co/settings/tokens にアクセス
2. "New token" をクリック
3. トークンの設定:
   - **Name**: `aiminer-github-actions` (任意の名前)
   - **Role**: `Write` を選択 (読み書き権限)
4. "Generate token" をクリック
5. **重要**: 表示されたトークンをコピーして安全に保存

---

## ステップ2: Hugging Faceリポジトリの作成

### 2.1 新しいモデルリポジトリを作成
1. https://huggingface.co/new にアクセス
2. 以下を設定:
   - **Owner**: あなたのユーザー名 (例: `imuzen127`)
   - **Model name**: `gpt-oss-20b-GGUF` (任意の名前)
   - **License**: `mit` または適切なライセンス
   - **Visibility**: `Public` または `Private`
3. "Create model" をクリック

作成されたリポジトリURL例: `https://huggingface.co/imuzen127/gpt-oss-20b-GGUF`

---

## ステップ3: モデルファイルのアップロード

### 方法A: Hugging Face CLI を使う (推奨・大容量ファイル向け)

#### 3.1 Hugging Face CLIのインストール
Windows PowerShellまたはコマンドプロンプトで:
```bash
pip install huggingface-hub[cli]
```

#### 3.2 ログイン
```bash
huggingface-cli login
```
先ほど作成したトークンを入力します。

#### 3.3 モデルファイルのアップロード
```bash
cd "C:\Users\imuze\.lmstudio\models\lmstudio-community\gpt-oss-20b-GGUF"

huggingface-cli upload imuzen127/gpt-oss-20b-GGUF gpt-oss-20b-MXFP4.gguf
```

**注意**: 12GBのアップロードには時間がかかります（回線速度により1時間〜数時間）。

---

### 方法B: Web UIを使う (小さなファイル向け)

1. https://huggingface.co/imuzen127/gpt-oss-20b-GGUF にアクセス
2. "Files" タブをクリック
3. "Add file" → "Upload files" をクリック
4. `gpt-oss-20b-MXFP4.gguf` をドラッグ&ドロップ
5. "Commit changes to main" をクリック

**注意**: Web UIは大容量ファイルでタイムアウトする可能性があるため、CLI推奨。

---

## ステップ4: モデルカードの作成 (オプションだが推奨)

リポジトリに `README.md` を追加して、モデルの説明を記載します:

```markdown
---
license: mit
---

# GPT-OSS 20B GGUF Model

This is a quantized GGUF model for use with llama.cpp and compatible inference engines.

## Model Details

- **Model**: GPT-OSS 20B
- **Quantization**: MXFP4
- **File Size**: 12GB
- **Format**: GGUF

## Usage

### With llama.cpp
\`\`\`bash
./main -m gpt-oss-20b-MXFP4.gguf -p "Your prompt here"
\`\`\`

### With Python (llama-cpp-python)
\`\`\`python
from llama_cpp import Llama

llm = Llama(model_path="gpt-oss-20b-MXFP4.gguf")
output = llm("Your prompt here")
print(output)
\`\`\`

## License

MIT License
```

---

## ステップ5: GitHub Secretsの設定

GitHub ActionsからHugging Faceにアクセスするため、トークンをGitHub Secretsに登録します。

### 5.1 GitHubリポジトリの設定ページに移動
https://github.com/imuzen127/AIminer/settings/secrets/actions

### 5.2 新しいシークレットを追加
1. "New repository secret" をクリック
2. 以下を入力:
   - **Name**: `HF_TOKEN`
   - **Value**: Hugging Faceで作成したアクセストークンを貼り付け
3. "Add secret" をクリック

---

## ステップ6: GitHub Actionsワークフローの更新

`.github/workflows/ai-brain-process.yml` を編集し、Hugging Faceのリポジトリ情報を設定します。

以下の行を探して、コメントを外して編集:

```yaml
# 変更前
# huggingface-cli download YOUR_USERNAME/YOUR_MODEL_NAME gpt-oss-20b-MXFP4.gguf --local-dir ./models --token $HF_TOKEN

# 変更後
huggingface-cli download imuzen127/gpt-oss-20b-GGUF gpt-oss-20b-MXFP4.gguf --local-dir ./models --token $HF_TOKEN
```

また、brain_processor.pyを実行する行も有効化:

```yaml
# 変更前
# python brain_processor.py ../models/gpt-oss-20b-MXFP4.gguf "${BRAIN_PATH}"

# 変更後
python brain_processor.py ../models/gpt-oss-20b-MXFP4.gguf "${BRAIN_PATH}"
```

---

## ステップ7: 動作確認

### 7.1 手動でワークフローを実行
1. https://github.com/imuzen127/AIminer/actions にアクセス
2. "AI Brain Processing" ワークフローを選択
3. "Run workflow" をクリック
4. "Run workflow" ボタンをクリック

### 7.2 ログを確認
- モデルが正しくダウンロードされているか
- brain_processor.pyが正常に動作しているか
- brain.jsonが更新されているか

---

## トラブルシューティング

### アップロードが失敗する
- **原因**: ネットワークタイムアウト
- **解決策**: CLIで `--resume` オプションを使う:
  ```bash
  huggingface-cli upload imuzen127/gpt-oss-20b-GGUF gpt-oss-20b-MXFP4.gguf --resume
  ```

### GitHub Actionsでダウンロードが失敗する
- **原因**: トークンが正しく設定されていない
- **解決策**:
  1. GitHub Secretsで `HF_TOKEN` が正しく設定されているか確認
  2. トークンに `Write` 権限があるか確認

### モデルのダウンロードに時間がかかりすぎる
- **原因**: GitHub Actionsの無料枠はネットワーク速度に制限あり
- **解決策**:
  1. 小さいモデルで試す
  2. GitHub Actions Cacheを使ってモデルをキャッシュする

---

## まとめ

1. Hugging Faceでアカウント作成とトークン取得
2. Hugging Faceにモデルリポジトリを作成
3. CLIまたはWeb UIでモデルをアップロード
4. GitHub Secretsに `HF_TOKEN` を登録
5. ワークフローファイルを更新
6. GitHub Actionsで動作確認

これで、GitHub Actions上でHugging Faceからモデルをダウンロードし、AI推論を実行できるようになります。
