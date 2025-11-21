package plugin.midorin.info.aIminer.executor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.model.Task;
import plugin.midorin.info.aIminer.model.TaskStatus;

import java.util.logging.Logger;

/**
 * タスクを実行するクラス
 * brain.jsonのtasksセクションを監視し、PENDINGタスクを自動実行
 */
public class TaskExecutor {
    private final JavaPlugin plugin;
    private final BrainFileManager brainFileManager;
    private final BotManager botManager;
    private final Logger logger;
    private int taskId = -1;

    public TaskExecutor(JavaPlugin plugin, BrainFileManager brainFileManager, BotManager botManager) {
        this.plugin = plugin;
        this.brainFileManager = brainFileManager;
        this.botManager = botManager;
        this.logger = plugin.getLogger();
    }

    /**
     * タスク実行ループを開始（20tick = 1秒ごとにチェック）
     */
    public void startTaskLoop() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            processNextTask();
        }, 0L, 20L); // 0tick後に開始、20tickごとに実行
    }

    /**
     * 次のPENDINGタスクを処理
     */
    private void processNextTask() {
        Task task = brainFileManager.getNextPendingTask();

        if (task == null) {
            return; // タスクがない場合は何もしない
        }

        logger.info("Executing task: " + task.getType() + " (ID: " + task.getId() + ")");

        // タスクをIN_PROGRESSに変更
        brainFileManager.updateTaskStatus(task.getId(), TaskStatus.IN_PROGRESS);

        // タスクタイプに応じて実行
        boolean success = executeTask(task);

        if (success) {
            // 成功したらCOMPLETED
            brainFileManager.updateTaskStatus(task.getId(), TaskStatus.COMPLETED);
            logger.info("Task completed successfully: " + task.getId());
        } else {
            // 失敗したらFAILED
            brainFileManager.updateTaskStatus(task.getId(), TaskStatus.FAILED);
            logger.warning("Task failed: " + task.getId());
        }

        // 完了・失敗タスクを削除（タスク履歴をクリーンに保つ）
        brainFileManager.removeCompletedTasks();

        // 脳ファイルを保存
        brainFileManager.saveBrainFile();
    }

    /**
     * タスクを実際に実行
     */
    private boolean executeTask(Task task) {
        try {
            switch (task.getType()) {
                case MINE_WOOD:
                    return executeMineWood(task);

                case MINE_STONE:
                    return executeMineStone(task);

                case MOVE_TO:
                    return executeMoveTo(task);

                case CHAT:
                    return executeChat(task);

                case GET_INVENTORY:
                    return executeGetInventory(task);

                case GET_POSITION:
                    return executeGetPosition(task);

                case WAIT:
                    return executeWait(task);

                default:
                    logger.warning("Unknown task type: " + task.getType());
                    return false;
            }
        } catch (Exception e) {
            logger.severe("Error executing task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 木を掘るタスク
     */
    private boolean executeMineWood(Task task) {
        int x = getIntParameter(task.getParameters(), "x");
        int y = getIntParameter(task.getParameters(), "y");
        int z = getIntParameter(task.getParameters(), "z");

        CommandSender executor = getTaskExecutor();
        if (executor == null) {
            logger.warning("No valid command executor available");
            return false;
        }

        String command = String.format("function imuzen127x74:xoak {x:%d,y:%d,z:%d}", x, y, z);

        logger.info("Executing as " + executor.getName() + ": " + command);
        return Bukkit.dispatchCommand(executor, command);
    }

    /**
     * 石を掘るタスク
     */
    private boolean executeMineStone(Task task) {
        int x = getIntParameter(task.getParameters(), "x");
        int y = getIntParameter(task.getParameters(), "y");
        int z = getIntParameter(task.getParameters(), "z");

        CommandSender executor = getTaskExecutor();
        if (executor == null) {
            logger.warning("No valid command executor available");
            return false;
        }

        String command = String.format("function imuzen127x74:xstone {x:%d,y:%d,z:%d}", x, y, z);

        logger.info("Executing as " + executor.getName() + ": " + command);
        return Bukkit.dispatchCommand(executor, command);
    }

    /**
     * 移動タスク
     */
    private boolean executeMoveTo(Task task) {
        int x = getIntParameter(task.getParameters(), "x");
        int y = getIntParameter(task.getParameters(), "y");
        int z = getIntParameter(task.getParameters(), "z");

        CommandSender executor = getTaskExecutor();
        if (executor == null) {
            logger.warning("No valid command executor available");
            return false;
        }

        String command = String.format("function imuzen127x74:xaim {x:%d,y:%d,z:%d}", x, y, z);

        logger.info("Executing as " + executor.getName() + ": " + command);
        return Bukkit.dispatchCommand(executor, command);
    }

    /**
     * タスク実行者を取得（ボットオーナーまたはオンラインプレイヤー）
     */
    private CommandSender getTaskExecutor() {
        CommandSender owner = botManager.getBotOwner();
        if (owner != null) {
            return owner;
        }

        // フォールバック：オンラインの最初のプレイヤー
        return Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
    }

    /**
     * タスクパラメータから整数値を安全に取得
     * JSONパース後はDouble型になるため、適切に変換する
     */
    private int getIntParameter(java.util.Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            logger.warning("Parameter '" + key + "' is null, defaulting to 0");
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warning("Parameter '" + key + "' is not a valid number: " + value);
                return 0;
            }
        }

        logger.warning("Parameter '" + key + "' has unexpected type: " + value.getClass().getName());
        return 0;
    }

    /**
     * チャット送信タスク
     */
    private boolean executeChat(Task task) {
        String message = (String) task.getParameters().get("message");

        String command = "say [Bot] " + message;

        logger.info("Executing: " + command);
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * インベントリ取得タスク
     */
    private boolean executeGetInventory(Task task) {
        String command = "data get entity @e[tag=test1,limit=1] data.Inventory";

        logger.info("Executing: " + command);
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * 位置取得タスク
     */
    private boolean executeGetPosition(Task task) {
        String command = "data get entity @e[tag=test1,limit=1] Pos";

        logger.info("Executing: " + command);
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * 待機タスク
     */
    private boolean executeWait(Task task) {
        logger.info("Waiting...");
        return true;
    }
}
