package plugin.midorin.info.aIminer.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import plugin.midorin.info.aIminer.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public AIServerClient(String apiUrl, Logger logger) {
        this.apiUrl = apiUrl;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Configure HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
            requestJson.addProperty("temperature", 0.7);
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
        for (String task : rules.getAvailableTasks()) {
            prompt.append("- ").append(task).append("\n");
        }

        prompt.append("\n## 応答フォーマット\n");
        prompt.append("以下のJSON形式で応答してください：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"thought\": \"状況分析と判断理由\",\n");
        prompt.append("  \"memory_updates\": {\"key\": \"value\"},\n");
        prompt.append("  \"new_task\": {\n");
        prompt.append("    \"type\": \"TASK_TYPE\",\n");
        prompt.append("    \"parameters\": {\"param\": \"value\"},\n");
        prompt.append("    \"reason\": \"このタスクを実行する理由\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("\nタスクが不要な場合はnew_taskをnullにしてください。\n");

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

        // Vision - Blocks
        message.append("### 周囲のブロック\n");
        BlockVisionData blocks = brainData.getVision().getBlocks();

        // Get position from memory
        Memory memory = brainData.getMemory();
        Object posObj = memory.get("current_position");
        if (posObj instanceof Position) {
            Position pos = (Position) posObj;
            message.append(String.format("現在位置: x=%.1f, y=%.1f, z=%.1f\n",
                pos.getX(), pos.getY(), pos.getZ()));
        }

        if (blocks != null) {
            if (blocks.getViewDirection() != null) {
                ViewDirection dir = blocks.getViewDirection();
                message.append(String.format("視線方向: yaw=%.1f, pitch=%.1f\n",
                    dir.getYaw(), dir.getPitch()));
            }

            if (blocks.getVisibleBlocks() != null && !blocks.getVisibleBlocks().isEmpty()) {
                message.append("近くのブロック (重要なもの):\n");
                // 最大20件表示、空気と一般的なブロックを除外
                int count = 0;
                for (VisibleBlock block : blocks.getVisibleBlocks()) {
                    if (count >= 20) {
                        break;
                    }
                    String blockType = block.getBlockType();
                    // 空気と一般的なブロックを除外
                    if (blockType == null || blockType.contains("AIR") ||
                        blockType.equals("GRASS_BLOCK") || blockType.equals("DIRT") ||
                        blockType.equals("STONE")) {
                        continue;
                    }
                    Position worldPos = block.getWorldPosition();
                    if (worldPos != null) {
                        message.append(String.format("  - %s (%.0f, %.0f, %.0f)\n",
                            blockType, worldPos.getX(), worldPos.getY(), worldPos.getZ()));
                    } else {
                        message.append(String.format("  - %s\n", blockType));
                    }
                    count++;
                }
                if (count == 0) {
                    message.append("  特筆すべきブロックなし\n");
                }
            }
        } else {
            message.append("ブロック情報なし\n");
        }
        message.append("\n");

        // Memory
        message.append("### メモリ\n");
        if (memory == null || memory.getData().isEmpty()) {
            message.append("空\n");
        } else {
            for (Map.Entry<String, Object> entry : memory.getData().entrySet()) {
                message.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
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
            }

            return brainData;

        } catch (Exception e) {
            logger.warning("Failed to parse AI response: " + e.getMessage());
            e.printStackTrace();
            return brainData;
        }
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
