package plugin.midorin.info.aIminer.vision;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.model.BlockVisionData;
import plugin.midorin.info.aIminer.model.Position;
import plugin.midorin.info.aIminer.model.ViewDirection;
import plugin.midorin.info.aIminer.model.VisibleBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * ボットの視覚システム - 周囲のブロックをスキャンする
 */
public class VisionScanner {
    private final JavaPlugin plugin;
    private static final int DEFAULT_SCAN_RADIUS = 10;
    private static final int VERTICAL_SCAN_RANGE = 5; // 上下のスキャン範囲

    public VisionScanner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * ボットエンティティの周囲をスキャンしてBlockVisionDataを生成
     *
     * @param botLocation ボットの位置
     * @param scanRadius スキャン半径（デフォルト: 10）
     * @return BlockVisionData 視覚情報
     */
    public BlockVisionData scanSurroundings(Location botLocation, int scanRadius) {
        if (botLocation == null || botLocation.getWorld() == null) {
            plugin.getLogger().warning("Invalid bot location for vision scanning");
            return new BlockVisionData();
        }

        BlockVisionData visionData = new BlockVisionData();
        visionData.setViewDistance(scanRadius);

        // ボットの向きを取得（yaw, pitch）
        ViewDirection viewDirection = new ViewDirection(
            botLocation.getYaw(),
            botLocation.getPitch()
        );
        visionData.setViewDirection(viewDirection);

        // 周囲のブロックをスキャン
        List<VisibleBlock> visibleBlocks = scanBlocks(botLocation, scanRadius);
        visionData.setVisibleBlocks(visibleBlocks);

        plugin.getLogger().info(String.format(
            "Vision scan completed: %d blocks found (radius: %d)",
            visibleBlocks.size(), scanRadius
        ));

        return visionData;
    }

    /**
     * デフォルトの半径でスキャン
     */
    public BlockVisionData scanSurroundings(Location botLocation) {
        return scanSurroundings(botLocation, DEFAULT_SCAN_RADIUS);
    }

    /**
     * 指定位置の周囲のブロックをスキャン
     */
    private List<VisibleBlock> scanBlocks(Location center, int radius) {
        List<VisibleBlock> blocks = new ArrayList<>();
        World world = center.getWorld();

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // 立方体領域をスキャン（最適化: AIRブロックは除外）
        for (int x = -radius; x <= radius; x++) {
            for (int y = -VERTICAL_SCAN_RANGE; y <= VERTICAL_SCAN_RANGE; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);

                    // AIRブロックはスキップ（情報量削減）
                    if (block.getType() == Material.AIR) {
                        continue;
                    }

                    // 距離計算
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    // スキャン範囲外はスキップ（球体に近づける）
                    if (distance > radius) {
                        continue;
                    }

                    // VisibleBlockオブジェクト作成
                    Position relativePos = new Position(x, y, z);
                    Position worldPos = new Position(
                        centerX + x,
                        centerY + y,
                        centerZ + z
                    );

                    VisibleBlock visibleBlock = new VisibleBlock(
                        relativePos,
                        worldPos,
                        block.getType().toString(),
                        Math.round(distance * 100.0) / 100.0 // 小数点2桁に丸める
                    );

                    blocks.add(visibleBlock);
                }
            }
        }

        return blocks;
    }

    /**
     * プレイヤーの位置を基準にスキャン（ボットエンティティが見つからない場合の代替）
     */
    public BlockVisionData scanFromPlayer(Player player, int scanRadius) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Invalid player for vision scanning");
            return new BlockVisionData();
        }

        return scanSurroundings(player.getLocation(), scanRadius);
    }

    /**
     * マネキン（ボット）エンティティを検索して位置を取得
     *
     * @param world スキャン対象のワールド
     * @return ボットの位置（見つからない場合はnull）
     */
    public Location findBotLocation(World world) {
        if (world == null) {
            return null;
        }

        // "imuzen127x74" タグを持つエンティティを検索
        // データパックで召喚されたマネキンを探す
        for (Entity entity : world.getEntities()) {
            // ArmorStandを探す（データパックで召喚されたマネキン）
            if (entity.getType().toString().equals("ARMOR_STAND")) {
                // カスタム名やタグでフィルタリング
                if (entity.getScoreboardTags().contains("aiminer_bot") ||
                    entity.getScoreboardTags().contains("imuzen127x74")) {
                    return entity.getLocation();
                }
            }
        }

        plugin.getLogger().fine("Bot entity not found in world");
        return null;
    }

    /**
     * 重要なブロックタイプのフィルタリング（オプション）
     * AIの判断に必要な鉱石、木材、ランドマークなどを優先的に記録
     */
    private boolean isImportantBlock(Material material) {
        // 鉱石類
        if (material.toString().contains("ORE")) return true;
        // 木材類
        if (material.toString().contains("LOG") || material.toString().contains("LEAVES")) return true;
        // ランドマーク
        if (material == Material.CHEST || material == Material.CRAFTING_TABLE) return true;
        // 地形の重要要素
        if (material == Material.WATER || material == Material.LAVA) return true;

        return false;
    }
}
