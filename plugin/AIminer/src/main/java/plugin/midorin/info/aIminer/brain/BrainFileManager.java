package plugin.midorin.info.aIminer.brain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import plugin.midorin.info.aIminer.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BrainFileManager {
    private final Path brainFilePath;
    private final Gson gson;
    private BrainData brainData;
    private static final int MAX_CHAT_HISTORY = 20;
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault());

    public BrainFileManager(File dataFolder) {
        this.brainFilePath = new File(dataFolder, "brain.json").toPath();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.brainData = new BrainData();
    }

    /**
     * 脳ファイルを初期化（新規作成）
     */
    public void initializeBrainFile() {
        this.brainData = new BrainData();
        saveBrainFile();
    }

    /**
     * 脳ファイルを読み込む
     */
    public BrainData loadBrainFile() {
        if (!Files.exists(brainFilePath)) {
            initializeBrainFile();
            return brainData;
        }

        try (Reader reader = new FileReader(brainFilePath.toFile())) {
            brainData = gson.fromJson(reader, BrainData.class);
            if (brainData == null) {
                brainData = new BrainData();
            }
            return brainData;
        } catch (IOException e) {
            e.printStackTrace();
            brainData = new BrainData();
            return brainData;
        }
    }

    /**
     * 脳ファイルを保存
     */
    public void saveBrainFile() {
        try {
            Files.createDirectories(brainFilePath.getParent());
            try (Writer writer = new FileWriter(brainFilePath.toFile())) {
                gson.toJson(brainData, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * チャットメッセージを追加
     */
    public void addChatMessage(String player, String message, long timestamp) {
        String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
        ChatMessage chatMessage = new ChatMessage(timeStr, player, message);

        List<ChatMessage> chatHistory = brainData.getVision().getChat();
        chatHistory.add(chatMessage);

        // 最新20件のみ保持
        if (chatHistory.size() > MAX_CHAT_HISTORY) {
            chatHistory.remove(0);
        }
    }

    /**
     * ブロック視覚情報を更新
     */
    public void updateBlockVision(BlockVisionData blockVisionData) {
        brainData.getVision().setBlocks(blockVisionData);
    }

    /**
     * メモリ情報を更新
     */
    public void updateMemory(String key, Object value) {
        brainData.getMemory().put(key, value);
    }

    /**
     * タスクを追加
     */
    public void addTask(Task task) {
        brainData.getTasks().add(task);
    }

    /**
     * 最初のpendingタスクを取得
     */
    public Task getNextPendingTask() {
        for (Task task : brainData.getTasks()) {
            if (task.getStatus() == TaskStatus.PENDING) {
                return task;
            }
        }
        return null;
    }

    /**
     * タスクのステータスを更新
     */
    public void updateTaskStatus(int taskId, TaskStatus status) {
        for (Task task : brainData.getTasks()) {
            if (task.getId() == taskId) {
                task.setStatus(status);
                break;
            }
        }
    }

    /**
     * 完了・失敗したタスクを削除
     */
    public void removeCompletedTasks() {
        brainData.getTasks().removeIf(task ->
            task.getStatus() == TaskStatus.COMPLETED ||
            task.getStatus() == TaskStatus.FAILED);
    }

    /**
     * 現在の脳データを取得
     */
    public BrainData getBrainData() {
        return brainData;
    }

    /**
     * 脳データを設定（AIサーバーからの更新時）
     */
    public void setBrainData(BrainData brainData) {
        this.brainData = brainData;
    }
}
