package plugin.midorin.info.aIminer.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugin.midorin.info.aIminer.ai.AIProcessingTask;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /bot コマンドのハンドラー
 */
public class BotCommand implements CommandExecutor, TabCompleter {
    private final BotManager botManager;
    private final BrainFileManager brainFileManager;
    private final AIProcessingTask aiProcessingTask;

    public BotCommand(BotManager botManager, BrainFileManager brainFileManager, AIProcessingTask aiProcessingTask) {
        this.botManager = botManager;
        this.brainFileManager = brainFileManager;
        this.aiProcessingTask = aiProcessingTask;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e=== AIminer Bot Commands ===");
            sender.sendMessage("§a/bot start §7- ボットを起動");
            sender.sendMessage("§a/bot status §7- ボットの状態を確認");
            sender.sendMessage("§a/bot brain §7- 脳ファイルの内容を表示");
            sender.sendMessage("§a/bot tasks §7- 現在のタスク一覧");
            sender.sendMessage("§a/bot think §7- AI処理を即座に実行");
            sender.sendMessage("§a/bot memory §7- メモリ内容を表示");
            sender.sendMessage("§a/bot chat §7- チャット履歴を表示");
            sender.sendMessage("§a/bot reset §7- ボットをリセット");
            sender.sendMessage("§a/bot test §7- テストタスクを追加");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                return handleStart(sender);

            case "status":
                return handleStatus(sender);

            case "brain":
                return handleBrain(sender);

            case "tasks":
                return handleTasks(sender);

            case "think":
                return handleThink(sender);

            case "memory":
                return handleMemory(sender);

            case "chat":
                return handleChat(sender);

            case "reset":
                return handleReset(sender);

            case "test":
                return handleTest(sender, args);

            default:
                sender.sendMessage("§cUnknown subcommand. Use /bot for help.");
                return true;
        }
    }

    /**
     * /bot start - ボット起動
     */
    private boolean handleStart(CommandSender sender) {
        sender.sendMessage("§aStarting bot...");

        if (botManager.isBotSummoned()) {
            sender.sendMessage("§eBot is already running.");
            return true;
        }

        // コマンド実行者の座標でボットを召喚
        boolean success = botManager.summonBot(sender);

        if (success) {
            sender.sendMessage("§aBot started successfully at your location!");
            sender.sendMessage("§7Bot will process tasks automatically.");
        } else {
            sender.sendMessage("§cFailed to start bot.");
        }

        return true;
    }

    /**
     * /bot status - ステータス確認
     */
    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage("§e=== Bot Status ===");
        sender.sendMessage("§7Summoned: " + (botManager.isBotSummoned() ? "§aYes" : "§cNo"));

        // タスク数を確認
        BrainData brain = brainFileManager.getBrainData();
        int totalTasks = brain.getTasks().size();
        int pendingTasks = (int) brain.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .count();

        sender.sendMessage("§7Total Tasks: §e" + totalTasks);
        sender.sendMessage("§7Pending Tasks: §e" + pendingTasks);

        Task nextTask = brainFileManager.getNextPendingTask();
        if (nextTask != null) {
            sender.sendMessage("§7Next Task: §e" + nextTask.getType() + " §7(ID: " + nextTask.getId() + ")");
        } else {
            sender.sendMessage("§7Next Task: §7None");
        }

        // AI処理状態
        sender.sendMessage("§7AI Processing: " + (aiProcessingTask != null ? "§aEnabled" : "§cDisabled"));

        return true;
    }

    /**
     * /bot brain - 脳ファイルの概要表示
     */
    private boolean handleBrain(CommandSender sender) {
        BrainData brain = brainFileManager.getBrainData();

        sender.sendMessage("§e=== Brain File Overview ===");

        // ルール
        BrainRules rules = brain.getRules();
        sender.sendMessage("§6[Rules]");
        sender.sendMessage("§7" + rules.getDescription());

        // メモリサイズ
        Memory memory = brain.getMemory();
        int memorySize = memory.getData().size();
        sender.sendMessage("§6[Memory] §7" + memorySize + " entries");

        // チャット履歴
        int chatSize = brain.getVision().getChat().size();
        sender.sendMessage("§6[Chat History] §7" + chatSize + " messages");

        // タスク
        int taskSize = brain.getTasks().size();
        sender.sendMessage("§6[Tasks] §7" + taskSize + " tasks");

        // 利用可能なタスクタイプ
        sender.sendMessage("§6[Available Task Types]");
        StringBuilder taskTypes = new StringBuilder("§7");
        for (String task : rules.getAvailableTasks()) {
            taskTypes.append(task).append(", ");
        }
        if (taskTypes.length() > 2) {
            taskTypes.setLength(taskTypes.length() - 2);
        }
        sender.sendMessage(taskTypes.toString());

        return true;
    }

    /**
     * /bot tasks - タスク一覧表示
     */
    private boolean handleTasks(CommandSender sender) {
        BrainData brain = brainFileManager.getBrainData();
        List<Task> tasks = brain.getTasks();

        sender.sendMessage("§e=== Current Tasks ===");

        if (tasks.isEmpty()) {
            sender.sendMessage("§7No tasks in queue.");
            return true;
        }

        for (Task task : tasks) {
            String statusColor = switch (task.getStatus()) {
                case PENDING -> "§e";
                case IN_PROGRESS -> "§6";
                case COMPLETED -> "§a";
                case FAILED -> "§c";
            };

            sender.sendMessage(String.format("%s[%s] §f%s §7(ID: %d)",
                    statusColor, task.getStatus(), task.getType(), task.getId()));

            if (task.getReason() != null && !task.getReason().isEmpty()) {
                sender.sendMessage("  §7Reason: " + task.getReason());
            }

            if (!task.getParameters().isEmpty()) {
                sender.sendMessage("  §7Params: " + task.getParameters());
            }
        }

        return true;
    }

    /**
     * /bot think - AI処理を即座に実行
     */
    private boolean handleThink(CommandSender sender) {
        if (aiProcessingTask == null) {
            sender.sendMessage("§cAI processing is disabled.");
            return true;
        }

        if (!botManager.isBotSummoned()) {
            sender.sendMessage("§cBot is not summoned. Use /bot start first.");
            return true;
        }

        sender.sendMessage("§aTriggering AI processing...");
        aiProcessingTask.triggerImmediateProcessing();
        sender.sendMessage("§7Check console for AI response.");

        return true;
    }

    /**
     * /bot memory - メモリ内容表示
     */
    private boolean handleMemory(CommandSender sender) {
        BrainData brain = brainFileManager.getBrainData();
        Memory memory = brain.getMemory();

        sender.sendMessage("§e=== Bot Memory ===");

        if (memory.getData().isEmpty()) {
            sender.sendMessage("§7Memory is empty.");
            return true;
        }

        for (Map.Entry<String, Object> entry : memory.getData().entrySet()) {
            String value = entry.getValue().toString();
            // 長い値は切り詰め
            if (value.length() > 50) {
                value = value.substring(0, 47) + "...";
            }
            sender.sendMessage("§6" + entry.getKey() + ": §7" + value);
        }

        return true;
    }

    /**
     * /bot chat - チャット履歴表示
     */
    private boolean handleChat(CommandSender sender) {
        BrainData brain = brainFileManager.getBrainData();
        List<ChatMessage> chatHistory = brain.getVision().getChat();

        sender.sendMessage("§e=== Chat History ===");

        if (chatHistory.isEmpty()) {
            sender.sendMessage("§7No chat messages recorded.");
            return true;
        }

        // 最新10件を表示
        int start = Math.max(0, chatHistory.size() - 10);
        for (int i = start; i < chatHistory.size(); i++) {
            ChatMessage msg = chatHistory.get(i);
            sender.sendMessage(String.format("§7[%s] §e%s§7: %s",
                    msg.getTimestamp(), msg.getPlayer(), msg.getMessage()));
        }

        if (chatHistory.size() > 10) {
            sender.sendMessage("§7... and " + (chatHistory.size() - 10) + " more messages");
        }

        return true;
    }

    /**
     * /bot reset - リセット
     */
    private boolean handleReset(CommandSender sender) {
        botManager.resetBot();
        brainFileManager.initializeBrainFile();
        sender.sendMessage("§aBot and brain file reset successfully.");
        return true;
    }

    /**
     * /bot test - テストタスクを追加
     */
    private boolean handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /bot test <task_type>");
            sender.sendMessage("§7Example: /bot test chat");
            return true;
        }

        String taskType = args[1].toUpperCase();

        try {
            TaskType type = TaskType.valueOf(taskType);

            // テストタスクを作成
            Task task = new Task();
            task.setId((int) (System.currentTimeMillis() % 10000));
            task.setType(type);
            task.setStatus(TaskStatus.PENDING);
            task.setCreatedAt(System.currentTimeMillis());
            task.setReason("Manual test task");
            task.setParameters(new HashMap<>());

            // タスクタイプに応じてパラメータを設定
            if (type == TaskType.MINE_WOOD || type == TaskType.MINE_STONE || type == TaskType.MOVE_TO) {
                Map<String, Object> params = new HashMap<>();
                params.put("x", 0);
                params.put("y", 64);
                params.put("z", 0);
                task.setParameters(params);
            } else if (type == TaskType.CHAT) {
                Map<String, Object> params = new HashMap<>();
                params.put("message", "Test message from bot");
                task.setParameters(params);
            }

            // タスクを追加
            brainFileManager.addTask(task);
            brainFileManager.saveBrainFile();

            sender.sendMessage("§aTest task added: " + type + " (ID: " + task.getId() + ")");
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid task type: " + taskType);
            sender.sendMessage("§7Available: MINE_WOOD, MINE_STONE, MOVE_TO, CHAT, GET_INVENTORY, GET_POSITION, GET_ENTITY_POSITION, READ_MEMORY");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("start");
            completions.add("status");
            completions.add("brain");
            completions.add("tasks");
            completions.add("think");
            completions.add("memory");
            completions.add("chat");
            completions.add("reset");
            completions.add("test");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            completions.add("chat");
            completions.add("move_to");
            completions.add("mine_wood");
            completions.add("mine_stone");
            completions.add("get_inventory");
            completions.add("get_position");
            completions.add("get_entity_position");
            completions.add("read_memory");
        }

        return completions;
    }
}
