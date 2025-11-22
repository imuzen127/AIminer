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
import plugin.midorin.info.aIminer.model.BlockVisionData;
import plugin.midorin.info.aIminer.model.Position;

/**
 * 定期的にボットの視覚情報を更新するタスク
 */
public class VisionUpdateTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final BrainFileManager brainFileManager;
    private final BotManager botManager;
    private final VisionScanner visionScanner;

    // 視覚更新の間隔（秒）
    private final int updateIntervalSeconds;
    private final int scanRadius;

    public VisionUpdateTask(
        JavaPlugin plugin,
        BrainFileManager brainFileManager,
        BotManager botManager,
        int scanRadius,
        int verticalScanRange,
        int updateIntervalSeconds
    ) {
        this.plugin = plugin;
        this.brainFileManager = brainFileManager;
        this.botManager = botManager;
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

        plugin.getLogger().fine(String.format(
            "Vision updated: %d blocks at location (%.1f, %.1f, %.1f)",
            visionData.getVisibleBlocks().size(),
            scanLocation.getX(),
            scanLocation.getY(),
            scanLocation.getZ()
        ));
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
