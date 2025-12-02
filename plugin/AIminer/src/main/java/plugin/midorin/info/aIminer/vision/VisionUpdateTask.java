package plugin.midorin.info.aIminer.vision;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.listener.DataCommandListener;
import plugin.midorin.info.aIminer.model.BlockVisionData;
import plugin.midorin.info.aIminer.model.Position;
import plugin.midorin.info.aIminer.model.VisibleEntity;
import plugin.midorin.info.aIminer.util.CommandResultCapture;

import java.util.ArrayList;
import java.util.List;

/**
 * 定期的にボットの視覚情報を更新するタスク
 */
public class VisionUpdateTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final BrainFileManager brainFileManager;
    private final BotManager botManager;
    private final DataCommandListener dataCommandListener;
    private final VisionScanner visionScanner;

    // 視覚更新の間隔（秒）
    private final int updateIntervalSeconds;
    private final int scanRadius;

    // ボットのタグ
    private static final String BOT_FEET_TAG = "test1";

    public VisionUpdateTask(
        JavaPlugin plugin,
        BrainFileManager brainFileManager,
        BotManager botManager,
        DataCommandListener dataCommandListener,
        int scanRadius,
        int verticalScanRange,
        int updateIntervalSeconds
    ) {
        this.plugin = plugin;
        this.brainFileManager = brainFileManager;
        this.botManager = botManager;
        this.dataCommandListener = dataCommandListener;
        this.scanRadius = scanRadius;
        this.updateIntervalSeconds = updateIntervalSeconds;
        this.visionScanner = new VisionScanner(plugin, verticalScanRange);
    }

    @Override
    public void run() {
        // ボットが召喚されていない場合はスキップ
        if (!botManager.isBotSummoned()) {
            return;
        }

        // ボットのオーナーを取得
        CommandSender owner = botManager.getBotOwner();
        if (owner == null) {
            plugin.getLogger().warning("Bot owner not found, skipping vision update");
            return;
        }

        Location scanLocation = null;

        // プレイヤーの場合は位置を取得
        if (owner instanceof Player) {
            Player player = (Player) owner;
            if (!player.isOnline()) {
                plugin.getLogger().warning("Bot owner is offline, skipping vision update");
                return;
            }

            // まずボットエンティティの位置を探す
            scanLocation = visionScanner.findBotLocation(player.getWorld());

            // ボットエンティティが見つからない場合はプレイヤー位置を使用
            if (scanLocation == null) {
                scanLocation = player.getLocation();
                plugin.getLogger().fine("Using player location for vision scan (bot entity not found)");
            }
        } else {
            // コンソールからの実行の場合はスキップ
            plugin.getLogger().warning("Bot owner is not a player, skipping vision update");
            return;
        }

        // 視覚情報をスキャン
        BlockVisionData visionData = visionScanner.scanSurroundings(scanLocation, scanRadius);

        // コマンド経由で近くのアイテムエンティティを取得
        List<VisibleEntity> nearbyItemsFromCommand = captureNearbyItems();
        if (!nearbyItemsFromCommand.isEmpty()) {
            List<VisibleEntity> existingItems = visionData.getNearbyItems();
            if (existingItems == null) {
                existingItems = new ArrayList<>();
            }
            existingItems.addAll(nearbyItemsFromCommand);
            visionData.setNearbyItems(existingItems);
        }

        // Brain Fileに保存
        brainFileManager.updateBlockVision(visionData);

        // ボット位置をMemoryに保存
        Position botPosition = new Position(
            (int) scanLocation.getX(),
            (int) scanLocation.getY(),
            (int) scanLocation.getZ()
        );
        brainFileManager.updateMemory("current_position", botPosition);
        brainFileManager.updateMemory("bot_position_source", "vision_scan");

        // インベントリを自動取得してMemoryに保存
        captureAndStoreInventory();

        // 近くのアイテム情報もMemoryに保存
        if (!nearbyItemsFromCommand.isEmpty()) {
            List<String> itemInfo = new ArrayList<>();
            for (VisibleEntity item : nearbyItemsFromCommand) {
                itemInfo.add(String.format("%s x%d at (%.1f, %.1f, %.1f)",
                    item.getName(), item.getCount(),
                    item.getWorldPosition().getX(),
                    item.getWorldPosition().getY(),
                    item.getWorldPosition().getZ()));
            }
            brainFileManager.updateMemory("nearby_items", itemInfo);
        } else {
            brainFileManager.updateMemory("nearby_items", new ArrayList<>());
        }

        brainFileManager.saveBrainFile();

        plugin.getLogger().fine(String.format(
            "Vision updated: %d blocks at location (%.1f, %.1f, %.1f)",
            visionData.getVisibleBlocks().size(),
            scanLocation.getX(),
            scanLocation.getY(),
            scanLocation.getZ()
        ));
    }

    /**
     * インベントリを自動取得してMemoryに保存
     */
    private void captureAndStoreInventory() {
        List<CommandResultCapture.InventoryItem> items =
            dataCommandListener.captureInventory(BOT_FEET_TAG);

        List<String> inventory = new ArrayList<>();
        if (items.isEmpty()) {
            inventory.add("empty");
        } else {
            for (CommandResultCapture.InventoryItem item : items) {
                inventory.add(item.getItemId() + " x" + item.getCount());
            }
        }
        brainFileManager.updateMemory("inventory", inventory);
        plugin.getLogger().fine("Inventory auto-captured: " + inventory);
    }

    /**
     * コマンド経由で近くのアイテムエンティティを取得
     */
    private List<VisibleEntity> captureNearbyItems() {
        List<VisibleEntity> items = new ArrayList<>();

        // @n[type=item] を使用して最も近いアイテムを取得
        CommandResultCapture.NearbyItem nearbyItem =
            dataCommandListener.captureNearbyItem(BOT_FEET_TAG);

        if (nearbyItem != null) {
            VisibleEntity entity = new VisibleEntity(
                new Position(nearbyItem.getX(), nearbyItem.getY(), nearbyItem.getZ()),
                "ITEM",
                nearbyItem.getItemId().toUpperCase(),
                nearbyItem.getCount(),
                0.0 // 距離は後で計算可能
            );
            items.add(entity);
            plugin.getLogger().info("Nearby item detected via command: " + nearbyItem);
        }

        return items;
    }

    /**
     * 視覚更新タスクを開始
     */
    public void startVisionLoop() {
        // 5秒ごとに実行（20tick = 1秒）
        long intervalTicks = updateIntervalSeconds * 20L;
        this.runTaskTimer(plugin, 20L, intervalTicks); // 1秒後に開始、その後指定秒ごと
        plugin.getLogger().info(String.format(
            "Vision update task started (interval: %d seconds, radius: %d blocks)",
            updateIntervalSeconds, scanRadius
        ));
    }

    /**
     * 視覚更新タスクを停止
     */
    public void stopVisionLoop() {
        this.cancel();
        plugin.getLogger().info("Vision update task stopped");
    }
}
