package plugin.midorin.info.aIminer.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import plugin.midorin.info.aIminer.model.*;
import plugin.midorin.info.aIminer.model.VisibleEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP Client for communicating with LM Studio (OpenAI-compatible API)
 */
public class AIServerClient {
    private final String apiUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Logger logger;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_SECONDS = 120; // 2 minutes for local LLM

    public AIServerClient(String apiUrl, Logger logger, int timeoutSeconds) {
        this.apiUrl = apiUrl;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;

        // Configure HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(effectiveTimeout, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Process brain data through LM Studio
     *
     * @param brainData Current brain state
     * @return Updated brain data with new task, or null if failed
     */
    public BrainData processBrain(BrainData brainData) {
        try {
            logger.info("Sending brain data to LM Studio: " + apiUrl);

            // Build system prompt from rules
            String systemPrompt = buildSystemPrompt(brainData.getRules());

            // Build user message from current state
            String userMessage = buildUserMessage(brainData);

            // Create OpenAI-compatible request
            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("model", "local-model");
            requestJson.addProperty("temperature", 0.3); // 低温度で確実な応答
            requestJson.addProperty("max_tokens", 512); // 短い応答で高速化

            JsonArray messages = new JsonArray();

            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);

            requestJson.add("messages", messages);

            String jsonBody = gson.toJson(requestJson);
            logger.fine("Request body: " + jsonBody);

            RequestBody body = RequestBody.create(jsonBody, JSON);

            // Build HTTP request to LM Studio endpoint
            Request httpRequest = new Request.Builder()
                    .url(apiUrl + "/v1/chat/completions")
                    .post(body)
                    .build();

            // Execute request
            long startTime = System.currentTimeMillis();
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                long responseTime = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    logger.warning(String.format(
                            "LM Studio returned error: %d %s",
                            response.code(),
                            response.message()
                    ));
                    return null;
                }

                // Parse response
                String responseBody = response.body().string();
                logger.fine("Response body: " + responseBody);

                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray choices = responseJson.getAsJsonArray("choices");

                if (choices == null || choices.size() == 0) {
                    logger.warning("No choices in LM Studio response");
                    return null;
                }

                String aiContent = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();

                logger.info(String.format("AI processing completed in %dms", responseTime));
                logger.info("AI Response: " + aiContent);

                // Parse AI response and update brain data
                return parseAIResponse(brainData, aiContent);
            }

        } catch (IOException e) {
            logger.severe("Failed to communicate with LM Studio: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.severe("Error processing brain data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Build system prompt from brain rules
     */
    private String buildSystemPrompt(BrainRules rules) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("あなたはMinecraftのボットAIです。\n\n");
        prompt.append("## 基本ルール\n");
        prompt.append(rules.getDescription()).append("\n\n");

        prompt.append("## 視覚情報のルール\n");
        prompt.append(rules.getVisionRules()).append("\n\n");

        prompt.append("## メモリのルール\n");
        prompt.append(rules.getMemoryRules()).append("\n\n");

        prompt.append("## タスクのルール\n");
        prompt.append(rules.getTaskRules()).append("\n\n");

        prompt.append("## 利用可能なタスク\n");
        prompt.append("【行動】\n");
        prompt.append("- CHAT: チャットで発言 {\"message\": \"発言内容\"}\n");
        prompt.append("- MINE_WOOD: 木を採取 {\"x\": 0, \"y\": 64, \"z\": 0}\n");
        prompt.append("- MINE_STONE: 石を採取 {\"x\": 0, \"y\": 64, \"z\": 0}\n");
        prompt.append("- MOVE_TO: 指定座標へ移動 {\"x\": 0, \"y\": 64, \"z\": 0} ※アイテムを拾う時も使用\n");
        prompt.append("【情報取得】\n");
        prompt.append("- GET_INVENTORY: 自分のインベントリを確認\n");
        prompt.append("- GET_POSITION: 自分の現在位置を確認\n");
        prompt.append("- GET_ENTITY_POSITION: エンティティ(プレイヤー等)の位置 {\"entity_name\": \"プレイヤー名\"}\n");
        prompt.append("- READ_MEMORY: メモリから情報を読み取る {\"key\": \"キー名\"}\n");
        prompt.append("\n## 重要な情報\n");
        prompt.append("- チャット履歴は自動で視覚情報に含まれます\n");
        prompt.append("- 周囲のアイテム（ドロップ品）はMOVE_TOで近づくと自動で拾えます\n");
        prompt.append("- 周囲のプレイヤー位置も視覚情報に含まれます\n");
        prompt.append("- 現在位置は視覚情報から直接取得できます（メモリのcurrent_positionより正確）\n");

        prompt.append("\n## 応答フォーマット\n");
        prompt.append("必ず以下のJSON形式で応答してください。余計な説明は不要です：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"thought\": \"状況分析（短く）\",\n");
        prompt.append("  \"memory_updates\": {},\n");
        prompt.append("  \"new_task\": {\n");
        prompt.append("    \"type\": \"CHAT\",\n");
        prompt.append("    \"parameters\": {\"message\": \"こんにちは！\"},\n");
        prompt.append("    \"reason\": \"挨拶に応答\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("\n重要な指示:\n");
        prompt.append("- チャット履歴にプレイヤーの発言があれば、必ずCHATタスクで応答してください\n");
        prompt.append("- new_taskは原則必須です。WAITは「他のアクションが進行中で待つ必要がある」場合のみ\n");
        prompt.append("- 何も依頼が無くても暇つぶし行動を提案してください: 近くの木/石を掘る、近づくためにMOVE_TOする、定期的にGET_POSITION/GET_INVENTORYする、状況報告をCHATする\n");
        prompt.append("- 本当に行動できる情報が無い時のみnew_taskをnullにしてよい\n");

        return prompt.toString();
    }

    /**
     * Build user message from current brain state
     */
    private String buildUserMessage(BrainData brainData) {
        StringBuilder message = new StringBuilder();

        message.append("## 現在の状態\n\n");

        // Vision - Chat
        message.append("### チャット履歴\n");
        List<ChatMessage> chatHistory = brainData.getVision().getChat();
        if (chatHistory.isEmpty()) {
            message.append("なし\n");
        } else {
            for (ChatMessage chat : chatHistory) {
                message.append(String.format("[%s] %s: %s\n",
                    chat.getTimestamp(), chat.getPlayer(), chat.getMessage()));
            }
        }
        message.append("\n");

        // Vision - Bot Position & Blocks
        message.append("### 自分の位置と周囲の状況\n");
        BlockVisionData blocks = brainData.getVision().getBlocks();

        // ボットの現在位置（リアルタイム）
        if (blocks != null && blocks.getBotPosition() != null) {
            Position botPos = blocks.getBotPosition();
            message.append(String.format("**現在位置**: x=%.1f, y=%.1f, z=%.1f\n",
                botPos.getX(), botPos.getY(), botPos.getZ()));
        } else {
            // フォールバック：メモリから取得
            Memory memory = brainData.getMemory();
            Object posObj = memory.get("current_position");
            if (posObj instanceof Position) {
                Position pos = (Position) posObj;
                message.append(String.format("現在位置(メモリ): x=%.1f, y=%.1f, z=%.1f\n",
                    pos.getX(), pos.getY(), pos.getZ()));
            } else {
                message.append("現在位置: 不明\n");
            }
        }

        if (blocks != null) {
            if (blocks.getViewDirection() != null) {
                ViewDirection dir = blocks.getViewDirection();
                message.append(String.format("視線方向: yaw=%.1f, pitch=%.1f\n",
                    dir.getYaw(), dir.getPitch()));
            }

            // 周囲のブロック（既にフィルタリング済み）
            if (blocks.getVisibleBlocks() != null && !blocks.getVisibleBlocks().isEmpty()) {
                message.append("\n**周囲のブロック**:\n");
                int count = 0;
                for (VisibleBlock block : blocks.getVisibleBlocks()) {
                    if (count >= 30) {
                        message.append("  ... (他にもあり)\n");
                        break;
                    }
                    String blockType = block.getBlockType();
                    if (blockType == null) continue;

                    Position worldPos = block.getWorldPosition();
                    if (worldPos != null) {
                        message.append(String.format("  - %s (%.0f, %.0f, %.0f) 距離:%.1f\n",
                            blockType, worldPos.getX(), worldPos.getY(), worldPos.getZ(),
                            block.getDistance()));
                    }
                    count++;
                }
                if (count == 0) {
                    message.append("  特筆すべきブロックなし\n");
                }
            } else {
                message.append("\n周囲に特筆すべきブロックなし\n");
            }

            // 周囲のドロップアイテム
            if (blocks.getNearbyItems() != null && !blocks.getNearbyItems().isEmpty()) {
                message.append("\n**周囲のアイテム（拾える）**:\n");
                for (VisibleEntity item : blocks.getNearbyItems()) {
                    Position pos = item.getWorldPosition();
                    message.append(String.format("  - %s x%d (%.1f, %.1f, %.1f) 距離:%.1f\n",
                        item.getName(), item.getCount(),
                        pos.getX(), pos.getY(), pos.getZ(),
                        item.getDistance()));
                }
                message.append("  ※MOVE_TOで近づくと自動で拾えます\n");
            }

            // 周囲のプレイヤー
            if (blocks.getNearbyPlayers() != null && !blocks.getNearbyPlayers().isEmpty()) {
                message.append("\n**周囲のプレイヤー**:\n");
                for (VisibleEntity player : blocks.getNearbyPlayers()) {
                    Position pos = player.getWorldPosition();
                    message.append(String.format("  - %s (%.1f, %.1f, %.1f) 距離:%.1f\n",
                        player.getName(),
                        pos.getX(), pos.getY(), pos.getZ(),
                        player.getDistance()));
                }
            }
        } else {
            message.append("ブロック情報なし\n");
        }
        message.append("\n");

        // Memory
        message.append("### メモリ\n");
        Memory memory = brainData.getMemory();
        if (memory == null || memory.getData().isEmpty()) {
            message.append("空\n");
        } else {
            for (Map.Entry<String, Object> entry : memory.getData().entrySet()) {
                // 長すぎるデータは省略
                String value = entry.getValue().toString();
                if (value.length() > 100) {
                    value = value.substring(0, 100) + "...";
                }
                message.append(String.format("- %s: %s\n", entry.getKey(), value));
            }
        }
        message.append("\n");

        // Current tasks
        message.append("### 現在のタスク\n");
        List<Task> tasks = brainData.getTasks();
        if (tasks.isEmpty()) {
            message.append("なし\n");
        } else {
            for (Task task : tasks) {
                message.append(String.format("- [%s] %s (ID: %d) - %s\n",
                    task.getStatus(), task.getType(), task.getId(), task.getReason()));
            }
        }

        message.append("\n次に何をすべきか判断してください。");

        return message.toString();
    }

    /**
     * Parse AI response and update brain data
     */
    private BrainData parseAIResponse(BrainData brainData, String aiContent) {
        try {
            // Extract JSON from response (might be wrapped in markdown code blocks)
            String jsonStr = extractJson(aiContent);
            if (jsonStr == null) {
                logger.warning("Could not extract JSON from AI response");
                return brainData;
            }

            JsonObject responseObj = JsonParser.parseString(jsonStr).getAsJsonObject();

            // Log thought process
            if (responseObj.has("thought")) {
                logger.info("AI Thought: " + responseObj.get("thought").getAsString());
            }

            // Update memory
            if (responseObj.has("memory_updates") && !responseObj.get("memory_updates").isJsonNull()) {
                JsonObject memoryUpdates = responseObj.getAsJsonObject("memory_updates");
                for (String key : memoryUpdates.keySet()) {
                    Object value = gson.fromJson(memoryUpdates.get(key), Object.class);
                    brainData.getMemory().put(key, value);
                    logger.info("Memory updated: " + key + " = " + value);
                }
            }

            // Add new task
            if (responseObj.has("new_task") && !responseObj.get("new_task").isJsonNull()) {
                JsonObject taskObj = responseObj.getAsJsonObject("new_task");

                Task newTask = new Task();
                newTask.setId(generateTaskId(brainData));

                String typeStr = taskObj.get("type").getAsString();
                try {
                    newTask.setType(TaskType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown task type: " + typeStr);
                    return brainData;
                }

                if (taskObj.has("parameters")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = gson.fromJson(
                        taskObj.get("parameters"), Map.class);
                    newTask.setParameters(params);
                }

                if (taskObj.has("reason")) {
                    newTask.setReason(taskObj.get("reason").getAsString());
                }

                newTask.setStatus(TaskStatus.PENDING);
                brainData.getTasks().add(newTask);

                logger.info(String.format("New task added: %s (ID: %d) - %s",
                    newTask.getType(), newTask.getId(), newTask.getReason()));
            } else {
                // フォールバック: AIがnullを返した場合でも行動する
                Task fallbackTask = createFallbackTask(brainData);
                if (fallbackTask != null) {
                    brainData.getTasks().add(fallbackTask);
                    logger.info(String.format("Fallback task added: %s (ID: %d) - %s",
                        fallbackTask.getType(), fallbackTask.getId(), fallbackTask.getReason()));
                }
            }

            return brainData;

        } catch (Exception e) {
            logger.warning("Failed to parse AI response: " + e.getMessage());
            e.printStackTrace();
            return brainData;
        }
    }

    private Task createFallbackTask(BrainData brainData) {
        // 1. 直近のチャットがあるなら簡易応答
        List<ChatMessage> chatHistory = brainData.getVision().getChat();
        if (!chatHistory.isEmpty() && brainData.getTasks().isEmpty()) {
            Task fallbackChat = new Task();
            fallbackChat.setId(generateTaskId(brainData));
            fallbackChat.setType(TaskType.CHAT);
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("message", "まだ行動指示がなければ周囲を見て動きますね。");
            fallbackChat.setParameters(params);
            fallbackChat.setReason("Fallback response to chat");
            fallbackChat.setStatus(TaskStatus.PENDING);
            return fallbackChat;
        }

        // 2. 近くのブロックに基づいて採掘タスクを作る
        BlockVisionData blockVisionData = brainData.getVision().getBlocks();
        if (blockVisionData != null && blockVisionData.getVisibleBlocks() != null) {
            for (VisibleBlock block : blockVisionData.getVisibleBlocks()) {
                String type = block.getBlockType();
                if (type == null) {
                    continue;
                }
                Position worldPos = block.getWorldPosition();
                if (worldPos == null) {
                    continue;
                }
                if (type.contains("LOG")) {
                    Task mineWood = new Task();
                    mineWood.setId(generateTaskId(brainData));
                    mineWood.setType(TaskType.MINE_WOOD);
                    Map<String, Object> params = new java.util.HashMap<>();
                    params.put("x", (int) worldPos.getX());
                    params.put("y", (int) worldPos.getY());
                    params.put("z", (int) worldPos.getZ());
                    mineWood.setParameters(params);
                    mineWood.setReason("Fallback: visible wood block");
                    mineWood.setStatus(TaskStatus.PENDING);
                    return mineWood;
                }
                if (type.equals("STONE") || type.contains("STONE")) {
                    Task mineStone = new Task();
                    mineStone.setId(generateTaskId(brainData));
                    mineStone.setType(TaskType.MINE_STONE);
                    Map<String, Object> params = new java.util.HashMap<>();
                    params.put("x", (int) worldPos.getX());
                    params.put("y", (int) worldPos.getY());
                    params.put("z", (int) worldPos.getZ());
                    mineStone.setParameters(params);
                    mineStone.setReason("Fallback: visible stone block");
                    mineStone.setStatus(TaskStatus.PENDING);
                    return mineStone;
                }
            }
        }

        // 3. 現在位置が分かるなら近場に移動して探索
        Object posObj = brainData.getMemory().get("current_position");
        if (posObj instanceof Position pos) {
            Task move = new Task();
            move.setId(generateTaskId(brainData));
            move.setType(TaskType.MOVE_TO);
            Map<String, Object> params = new java.util.HashMap<>();
            int dx = ThreadLocalRandom.current().nextInt(-5, 6);
            int dz = ThreadLocalRandom.current().nextInt(-5, 6);
            params.put("x", (int) pos.getX() + dx);
            params.put("y", (int) pos.getY());
            params.put("z", (int) pos.getZ() + dz);
            move.setParameters(params);
            move.setReason("Fallback: random exploration move");
            move.setStatus(TaskStatus.PENDING);
            return move;
        }

        // 4. それでも何もできなければ位置確認
        Task getPos = new Task();
        getPos.setId(generateTaskId(brainData));
        getPos.setType(TaskType.GET_POSITION);
        getPos.setParameters(new java.util.HashMap<>());
        getPos.setReason("Fallback: refresh position");
        getPos.setStatus(TaskStatus.PENDING);
        return getPos;
    }

    /**
     * Extract JSON from AI response (handles markdown code blocks)
     */
    private String extractJson(String content) {
        // Try to find JSON in code blocks
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
        Matcher matcher = codeBlockPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Try to find raw JSON object
        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
        matcher = jsonPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(0);
        }

        return null;
    }

    /**
     * Generate unique task ID
     */
    private int generateTaskId(BrainData brainData) {
        int maxId = 0;
        for (Task task : brainData.getTasks()) {
            if (task.getId() > maxId) {
                maxId = task.getId();
            }
        }
        return maxId + 1;
    }

    /**
     * Check if LM Studio server is healthy
     *
     * @return true if server is responsive
     */
    public boolean checkHealth() {
        try {
            // LM Studio uses /v1/models endpoint for health check
            Request request = new Request.Builder()
                    .url(apiUrl + "/v1/models")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("LM Studio health check: OK");
                    return true;
                } else {
                    logger.warning("LM Studio health check failed: " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            logger.warning("Cannot reach LM Studio: " + e.getMessage());
            return false;
        }
    }
}
