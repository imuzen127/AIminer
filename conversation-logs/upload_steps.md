# Hugging Face モデルアップロード - 実行手順

## 今すぐ実行する手順

### 1. Hugging Faceアカウントの準備

#### 1-1. Hugging Faceアカウントを持っていない場合
https://huggingface.co/join にアクセスして、アカウントを作成してください。

#### 1-2. アクセストークンの作成
1. https://huggingface.co/settings/tokens にアクセス
2. "New token" をクリック
3. 以下を設定:
   - **Name**: `aiminer` (任意)
   - **Role**: **Write** を選択（重要！）
4. "Generate token" をクリック
5. **表示されたトークンをコピー**（後で使います）

---

### 2. Hugging Faceにログイン

以下のコマンドを実行してください：

```bash
huggingface-cli login
```

トークンの入力を求められたら、コピーしたトークンを貼り付けてEnterを押してください。

---

### 3. モデルリポジトリの作成

#### 3-1. Web UIで作成（簡単）
1. https://huggingface.co/new にアクセス
2. 以下を入力:
   - **Owner**: あなたのユーザー名
   - **Model name**: `gpt-oss-20b-GGUF`
   - **License**: `mit`
   - **Visibility**: `Public` または `Private`（どちらでもOK）
3. "Create model" をクリック

**作成されたリポジトリのURLをメモしてください**（例: `https://huggingface.co/YOUR_USERNAME/gpt-oss-20b-GGUF`）

---

### 4. モデルファイルのアップロード

以下のコマンドを実行してください（YOUR_USERNAMEを実際のユーザー名に置き換え）：

```bash
cd "C:\Users\imuze\.lmstudio\models\lmstudio-community\gpt-oss-20b-GGUF"

huggingface-cli upload YOUR_USERNAME/gpt-oss-20b-GGUF gpt-oss-20b-MXFP4.gguf
```

**注意**:
- 12GBのアップロードには1〜3時間かかる可能性があります
- アップロード中は中断しないでください
- もし途中で切れた場合は、同じコマンドを再実行すれば続きから再開されます

---

### 5. アップロード完了後の確認

1. Hugging Faceのリポジトリページにアクセス
2. "Files" タブで `gpt-oss-20b-MXFP4.gguf` が表示されているか確認
3. ファイルサイズが約12GBになっているか確認

---

### 6. GitHub Secretsの設定

1. https://github.com/imuzen127/AIminer/settings/secrets/actions にアクセス
2. "New repository secret" をクリック
3. 以下を入力:
   - **Name**: `HF_TOKEN`
   - **Value**: 手順1-2で作成したHugging Faceトークン
4. "Add secret" をクリック

---

### 7. GitHub Actionsワークフローの更新

アップロードが完了したら、私に教えてください。
ワークフローファイルを更新して、Hugging Faceからモデルをダウンロードできるようにします。

**必要な情報**:
- あなたのHugging Faceユーザー名

---

## トラブルシューティング

### "401 Unauthorized" エラーが出る
- トークンが正しくない、またはWrite権限がない
- `huggingface-cli login` を再実行して、正しいトークンを入力

### アップロードが途中で止まる
- そのままコマンドを再実行すれば、続きから再開されます
- `huggingface-cli upload --resume YOUR_USERNAME/gpt-oss-20b-GGUF gpt-oss-20b-MXFP4.gguf`

### ネットワークが遅い
- アップロードには時間がかかるため、安定したネットワーク環境で実行してください
- 夜間など、ネットワークが空いている時間帯の実行を推奨
