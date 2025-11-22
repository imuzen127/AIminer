package plugin.midorin.info.aIminer.executor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.model.Task;
import plugin.midorin.info.aIminer.model.TaskStatus;
import plugin.midorin.info.aIminer.model.Position;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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

    // データパックのネームスペース（必要に応じて変更）
    private static final String DATAPACK_NS = "imuzen127x74";

    // データパックタグ（VisionScannerと揃える）
    private static final String BOT_FEET_TAG = "test1";
    private static final String BOT_BODY_TAG = "rider1";
    private static final String MOVE_MARKER_TAG = "aim1";
    private static final String WOOD_MARKER_TAG = "aim1o";
    private static final String STONE_MARKER_TAG = "aim1s";

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

                case GET_ENTITY_POSITION:
                    return executeGetEntityPosition(task);

                case READ_MEMORY:
                    return executeReadMemory(task);

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

        String setCommand = String.format("function %s:oakset {x:%d, y:%d, z:%d}", DATAPACK_NS, x, y, z);
        String onCommand = String.format("function %s:xoak_on", DATAPACK_NS);

        logger.info("Executing as CONSOLE: " + setCommand);
        boolean setOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setCommand);
        if (!setOk) {
            logger.warning("Failed to set oak target position");
            return false;
        }

        logger.info("Executing as CONSOLE: " + onCommand);
        boolean onOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), onCommand);
        if (!onOk) {
            logger.warning("Failed to enable oak continuous mining");
            return false;
        }

        return true;
    }

    /**
     * 石を掘るタスク
     */
    private boolean executeMineStone(Task task) {
        int x = getIntParameter(task.getParameters(), "x");
        int y = getIntParameter(task.getParameters(), "y");
        int z = getIntParameter(task.getParameters(), "z");

        String setCommand = String.format("function %s:stoneset {x:%d, y:%d, z:%d}", DATAPACK_NS, x, y, z);
        String onCommand = String.format("function %s:xstone_on", DATAPACK_NS);

        logger.info("Executing as CONSOLE: " + setCommand);
        boolean setOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setCommand);
        if (!setOk) {
            logger.warning("Failed to set stone target position");
            return false;
        }

        logger.info("Executing as CONSOLE: " + onCommand);
        boolean onOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), onCommand);
        if (!onOk) {
            logger.warning("Failed to enable stone continuous mining");
            return false;
        }

        return true;
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

        String command = String.format("function %s:xaim {x:%d, y:%d, z:%d}", DATAPACK_NS, x, y, z);

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
        LivingEntity botEntity = findBotEntity();
        if (botEntity == null) {
            logger.warning("Bot entity not found for inventory check");
            return false;
        }

        List<String> summary = new ArrayList<>();
        if (botEntity.getEquipment() != null) {
            summary.add("mainhand:" + botEntity.getEquipment().getItemInMainHand());
            summary.add("offhand:" + botEntity.getEquipment().getItemInOffHand());
            summary.add("helmet:" + botEntity.getEquipment().getHelmet());
            summary.add("chest:" + botEntity.getEquipment().getChestplate());
            summary.add("legs:" + botEntity.getEquipment().getLeggings());
            summary.add("boots:" + botEntity.getEquipment().getBoots());
        }

        brainFileManager.updateMemory("inventory_state", summary);
        brainFileManager.saveBrainFile();
        logger.info("Inventory snapshot stored in memory (equipment only)");
        return true;
    }

    /**
     * 位置取得タスク
     */
    private boolean executeGetPosition(Task task) {
        Location loc = findBotLocation();
        if (loc == null) {
            logger.warning("Bot location not found");
            return false;
        }

        Position pos = new Position(loc.getX(), loc.getY(), loc.getZ());
        brainFileManager.updateMemory("current_position", pos);
        brainFileManager.saveBrainFile();
        logger.info(String.format("Bot position stored: (%.1f, %.1f, %.1f)", pos.getX(), pos.getY(), pos.getZ()));
        return true;
    }

    /**
     * エンティティ（プレイヤー）位置取得タスク
     */
    private boolean executeGetEntityPosition(Task task) {
        Object nameObj = task.getParameters().getOrDefault("entity_name", task.getParameters().get("name"));
        if (!(nameObj instanceof String) || ((String) nameObj).isEmpty()) {
            logger.warning("GET_ENTITY_POSITION missing entity_name");
            return false;
        }

        String name = (String) nameObj;
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || !target.isOnline()) {
            logger.warning("Target player not online: " + name);
            return false;
        }

        Location loc = target.getLocation();
        Position pos = new Position(loc.getX(), loc.getY(), loc.getZ());
        brainFileManager.updateMemory("entity_position_" + name, pos);
        brainFileManager.saveBrainFile();

        String command = String.format("say [Bot] %s is at (%.1f, %.1f, %.1f)", name, pos.getX(), pos.getY(), pos.getZ());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        logger.info("Stored entity position for " + name);
        return true;
    }

    /**
     * メモリ読み出しタスク
     */
    private boolean executeReadMemory(Task task) {
        Object keyObj = task.getParameters().get("key");
        if (!(keyObj instanceof String) || ((String) keyObj).isEmpty()) {
            logger.warning("READ_MEMORY missing key");
            return false;
        }

        String key = (String) keyObj;
        Object value = brainFileManager.getBrainData().getMemory().get(key);
        String valueStr = value != null ? value.toString() : "null";
        String command = String.format("say [Bot memory] %s = %s", key, valueStr);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        logger.info("Memory read for key '" + key + "'");
        return true;
    }

    /**
     * 待機タスク
     */
    private boolean executeWait(Task task) {
        logger.info("Waiting...");
        return true;
    }

    private LivingEntity findBotEntity() {
        CommandSender owner = botManager.getBotOwner();
        World world = null;
        if (owner instanceof Player player && player.isOnline()) {
            world = player.getWorld();
        } else {
            world = Bukkit.getWorlds().stream().findFirst().orElse(null);
        }
        if (world == null) {
            return null;
        }
        Entity entity = findFirstEntityByTag(world, BOT_FEET_TAG);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        entity = findFirstEntityByTag(world, BOT_BODY_TAG);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private Location findBotLocation() {
        CommandSender owner = botManager.getBotOwner();
        World world = null;
        if (owner instanceof Player player && player.isOnline()) {
            world = player.getWorld();
        } else {
            world = Bukkit.getWorlds().stream().findFirst().orElse(null);
        }
        if (world == null) {
            return null;
        }

        Location loc = findFirstByTag(world, BOT_FEET_TAG);
        if (loc != null) return loc;
        loc = findFirstByTag(world, BOT_BODY_TAG);
        if (loc != null) return loc;
        loc = findFirstByTag(world, MOVE_MARKER_TAG);
        if (loc != null) return loc;
        loc = findFirstByTag(world, WOOD_MARKER_TAG);
        if (loc != null) return loc;
        return findFirstByTag(world, STONE_MARKER_TAG);
    }

    private Location findFirstByTag(org.bukkit.World world, String tag) {
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(tag)) {
                return entity.getLocation();
            }
        }
        return null;
    }

    private Entity findFirstEntityByTag(org.bukkit.World world, String tag) {
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(tag)) {
                return entity;
            }
        }
        return null;
    }
}
