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
import plugin.midorin.info.aIminer.model.TaskType;
import plugin.midorin.info.aIminer.model.Position;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        recordMineAction("MINE_WOOD", x, y, z);
        captureInventorySnapshot("after_mine_wood");

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

        recordMineAction("MINE_STONE", x, y, z);
        captureInventorySnapshot("after_mine_stone");

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
        boolean ok = Bukkit.dispatchCommand(executor, command);
        if (ok) {
            recordCurrentPosition(); // refresh memory with latest position after move command
            maybeEnqueueMineIfNearTarget();
        }
        return ok;
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
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (ok) {
            recordBotChat(message);
        }
        return ok;
    }

    /**
     * インベントリ取得タスク
     */
    private boolean executeGetInventory(Task task) {
        return captureInventorySnapshot("explicit_get_inventory");
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
        brainFileManager.updateMemory("current_position_ts", System.currentTimeMillis());
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

    private void recordMineAction(String type, int x, int y, int z) {
        Map<String, Object> info = new HashMap<>();
        info.put("type", type);
        info.put("x", x);
        info.put("y", y);
        info.put("z", z);
        info.put("timestamp", System.currentTimeMillis());
        storeMemoryAndSave("last_mine_action", info);
    }

    private void recordBotChat(String message) {
        Map<String, Object> chatInfo = new HashMap<>();
        chatInfo.put("message", message);
        chatInfo.put("timestamp", System.currentTimeMillis());
        storeMemoryAndSave("bot_last_chat", chatInfo);
    }

    private boolean captureInventorySnapshot(String reason) {
        String command = String.format("data get entity @e[tag=%s,limit=1] data.Inventory", BOT_FEET_TAG);
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        // Also gather equipment summary from the tagged entity if possible
        List<String> equipmentSummary = new ArrayList<>();
        LivingEntity entity = findBotEntity();
        if (entity != null && entity.getEquipment() != null) {
            equipmentSummary.add("mainhand:" + entity.getEquipment().getItemInMainHand());
            equipmentSummary.add("offhand:" + entity.getEquipment().getItemInOffHand());
            equipmentSummary.add("helmet:" + entity.getEquipment().getHelmet());
            equipmentSummary.add("chest:" + entity.getEquipment().getChestplate());
            equipmentSummary.add("legs:" + entity.getEquipment().getLeggings());
            equipmentSummary.add("boots:" + entity.getEquipment().getBoots());
        }

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("captured_at", System.currentTimeMillis());
        snapshot.put("reason", reason);
        snapshot.put("raw", new ArrayList<String>()); // raw output not captured; kept for compatibility
        snapshot.put("equipment", equipmentSummary);
        storeMemoryAndSave("inventory_snapshot", snapshot);
        recordCurrentPosition(); // update position alongside inventory

        if (!ok) {
            logger.warning("Inventory command failed: " + command);
        }
        logger.info("Inventory snapshot stored (equipment summary size: " + equipmentSummary.size() + ")");
        return ok;
    }

    private void storeMemoryAndSave(String key, Object value) {
        brainFileManager.updateMemory(key, value);
        brainFileManager.saveBrainFile();
    }

    private void recordCurrentPosition() {
        Location loc = findBotLocation();
        if (loc == null) {
            return;
        }
        Position pos = new Position(loc.getX(), loc.getY(), loc.getZ());
        brainFileManager.updateMemory("current_position", pos);
        brainFileManager.updateMemory("current_position_ts", System.currentTimeMillis());
        brainFileManager.saveBrainFile();
    }

    /**
     * If target_location is set and we are already near it, enqueue a mine task automatically.
     */
    private void maybeEnqueueMineIfNearTarget() {
        Object targetObj = brainFileManager.getBrainData().getMemory().get("target_location");
        if (!(targetObj instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) targetObj;
        Double tx = toDouble(target.get("x"));
        Double ty = toDouble(target.get("y"));
        Double tz = toDouble(target.get("z"));
        if (tx == null || ty == null || tz == null) {
            return;
        }

        Location loc = findBotLocation();
        if (loc == null) {
            return;
        }
        double dx = loc.getX() - tx;
        double dy = loc.getY() - ty;
        double dz = loc.getZ() - tz;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > 4.0) { // more than ~2 blocks away
            return;
        }

        TaskType mineType = TaskType.MINE_WOOD;
        Object tType = target.get("type");
        if (tType instanceof String typeStr) {
            if (typeStr.toUpperCase().contains("STONE")) {
                mineType = TaskType.MINE_STONE;
            } else if (typeStr.toUpperCase().contains("LOG")) {
                mineType = TaskType.MINE_WOOD;
            }
        }

        // avoid duplicate pending mine tasks for the same coords
        for (Task t : brainFileManager.getBrainData().getTasks()) {
            if (t.getType() == mineType && t.getStatus() == TaskStatus.PENDING) {
                Map<String, Object> p = t.getParameters();
                if (p != null &&
                    getIntParameter(p, "x") == tx.intValue() &&
                    getIntParameter(p, "y") == ty.intValue() &&
                    getIntParameter(p, "z") == tz.intValue()) {
                    return;
                }
            }
        }

        Task newTask = new Task();
        newTask.setId(generateTaskId());
        newTask.setType(mineType);
        Map<String, Object> params = new HashMap<>();
        params.put("x", tx.intValue());
        params.put("y", ty.intValue());
        params.put("z", tz.intValue());
        newTask.setParameters(params);
        newTask.setReason("Auto-mine target after arrival");
        newTask.setStatus(TaskStatus.PENDING);

        brainFileManager.addTask(newTask);
        brainFileManager.saveBrainFile();
        logger.info(String.format("Auto-enqueued %s at (%.0f, %.0f, %.0f)", mineType, tx, ty, tz));
    }

    private Double toDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private int generateTaskId() {
        int max = 0;
        for (Task t : brainFileManager.getBrainData().getTasks()) {
            if (t.getId() > max) {
                max = t.getId();
            }
        }
        return max + 1;
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

