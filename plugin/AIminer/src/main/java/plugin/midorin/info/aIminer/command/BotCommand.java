package plugin.midorin.info.aIminer.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.model.Task;
import plugin.midorin.info.aIminer.model.TaskStatus;
import plugin.midorin.info.aIminer.model.TaskType;

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

    public BotCommand(BotManager botManager, BrainFileManager brainFileManager) {
        this.botManager = botManager;
        this.brainFileManager = brainFileManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e=== AIminer Bot Commands ===");
            sender.sendMessage("§a/bot start §7- ボットを起動");
            sender.sendMessage("§a/bot status §7- ボットの状態を確認");
            sender.sendMessage("§a/bot reset §7- ボットをリセット");
            sender.sendMessage("§a/bot test §7- テストタスクを追加");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                return handleStart(sender);

            case "status":
                return handleStatus(sender);

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

        boolean success = botManager.summonBot();

        if (success) {
            sender.sendMessage("§aBot started successfully!");
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
        Task nextTask = brainFileManager.getNextPendingTask();
        if (nextTask != null) {
            sender.sendMessage("§7Next Task: §e" + nextTask.getType());
        } else {
            sender.sendMessage("§7Next Task: §7None");
        }

        return true;
    }

    /**
     * /bot reset - リセット
     */
    private boolean handleReset(CommandSender sender) {
        botManager.resetBot();
        sender.sendMessage("§aBot reset successfully.");
        return true;
    }

    /**
     * /bot test - テストタスクを追加
     */
    private boolean handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /bot test <task_type>");
            sender.sendMessage("§7Example: /bot test wait");
            return true;
        }

        String taskType = args[1].toUpperCase();

        try {
            TaskType type = TaskType.valueOf(taskType);

            // テストタスクを作成
            Task task = new Task();
            task.setId(0); // BrainFileManagerが自動採番
            task.setType(type);
            task.setStatus(TaskStatus.PENDING);
            task.setCreatedAt(System.currentTimeMillis());
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

            sender.sendMessage("§aTest task added: " + type);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid task type: " + taskType);
            sender.sendMessage("§7Available: MINE_WOOD, MINE_STONE, MOVE_TO, CHAT, GET_INVENTORY, GET_POSITION, WAIT");
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
            completions.add("reset");
            completions.add("test");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            completions.add("wait");
            completions.add("chat");
            completions.add("move_to");
            completions.add("mine_wood");
            completions.add("mine_stone");
            completions.add("get_inventory");
            completions.add("get_position");
        }

        return completions;
    }
}
