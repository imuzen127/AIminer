package plugin.midorin.info.aIminer.vision;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.model.BlockVisionData;
import plugin.midorin.info.aIminer.model.Position;
import plugin.midorin.info.aIminer.model.ViewDirection;
import plugin.midorin.info.aIminer.model.VisibleBlock;
import plugin.midorin.info.aIminer.model.VisibleEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * ボットの視覚システム - 周囲のブロック、アイテム、プレイヤーをスキャンする
 */
public class VisionScanner {
    private final JavaPlugin plugin;
    private static final int DEFAULT_SCAN_RADIUS = 10;
    private static final int MAX_IMPORTANT_BLOCKS = 30;  // 重要ブロックの最大数
    private static final int MAX_NORMAL_BLOCKS = 20;     // 通常ブロックの最大数

    // データパックで利用しているタグ定義
    private static final String BOT_FEET_TAG = "test1";      // ゾンビピグリン（実体）
    private static final String BOT_BODY_TAG = "rider1";     // マネキン（見た目）
    private static final String MOVE_MARKER_TAG = "aim1";    // 移動先マーカー
    private static final String WOOD_MARKER_TAG = "aim1o";   // 木掘りマーカー
    private static final String STONE_MARKER_TAG = "aim1s";  // 石掘りマーカー

    // スキャンから除外するブロック（情報価値が低い）
    private static final Set<Material> IGNORED_BLOCKS = EnumSet.of(
        Material.AIR,
        Material.CAVE_AIR,
        Material.VOID_AIR,
        Material.BEDROCK,       // 岩盤は掘れないので無視
        Material.GRASS_BLOCK,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.PODZOL,
        Material.SAND,
        Material.GRAVEL
    );

    // 重要なブロック（優先的に報告）
    private static final Set<Material> IMPORTANT_BLOCKS = EnumSet.of(
        // 木材系
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.CHERRY_LOG, Material.MANGROVE_LOG,
        // 鉱石系
        Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
        Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE,
        Material.REDSTONE_ORE, Material.COPPER_ORE,
        Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        // 有用ブロック
        Material.CHEST, Material.CRAFTING_TABLE, Material.FURNACE,
        Material.WATER, Material.LAVA
    );

    private final int verticalScanRange; // 上下のスキャン範囲

    public VisionScanner(JavaPlugin plugin) {
        this(plugin, 5);
    }

    public VisionScanner(JavaPlugin plugin, int verticalScanRange) {
        this.plugin = plugin;
        this.verticalScanRange = Math.max(1, verticalScanRange);
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

        // ボットの現在位置を記録
        visionData.setBotPosition(new Position(
            botLocation.getX(),
            botLocation.getY(),
            botLocation.getZ()
        ));

        // ボットの向きを取得（yaw, pitch）
        ViewDirection viewDirection = new ViewDirection(
            botLocation.getYaw(),
            botLocation.getPitch()
        );
        visionData.setViewDirection(viewDirection);

        // 周囲のブロックをスキャン（フィルタリング・優先度付き）
        List<VisibleBlock> visibleBlocks = scanBlocks(botLocation, scanRadius);
        visionData.setVisibleBlocks(visibleBlocks);

        // 周囲のドロップアイテムをスキャン
        List<VisibleEntity> nearbyItems = scanItems(botLocation, scanRadius);
        visionData.setNearbyItems(nearbyItems);

        // 周囲のプレイヤーをスキャン
        List<VisibleEntity> nearbyPlayers = scanPlayers(botLocation, scanRadius);
        visionData.setNearbyPlayers(nearbyPlayers);

        plugin.getLogger().info(String.format(
            "Vision scan completed: %d blocks, %d items, %d players (radius: %d)",
            visibleBlocks.size(), nearbyItems.size(), nearbyPlayers.size(), scanRadius
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
     * 指定位置の周囲のブロックをスキャン（フィルタリング・優先度付き）
     */
    private List<VisibleBlock> scanBlocks(Location center, int radius) {
        List<VisibleBlock> importantBlocks = new ArrayList<>();
        List<VisibleBlock> normalBlocks = new ArrayList<>();
        World world = center.getWorld();

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // 立方体領域をスキャン
        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalScanRange; y <= verticalScanRange; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);
                    Material material = block.getType();

                    // 無視するブロックはスキップ
                    if (IGNORED_BLOCKS.contains(material)) {
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
                        material.toString(),
                        Math.round(distance * 100.0) / 100.0
                    );

                    // 重要ブロックと通常ブロックを分けて管理
                    if (IMPORTANT_BLOCKS.contains(material)) {
                        importantBlocks.add(visibleBlock);
                    } else {
                        normalBlocks.add(visibleBlock);
                    }
                }
            }
        }

        // 距離でソート（近い順）
        importantBlocks.sort(Comparator.comparingDouble(VisibleBlock::getDistance));
        normalBlocks.sort(Comparator.comparingDouble(VisibleBlock::getDistance));

        // 結果を結合（重要ブロック優先、制限付き）
        List<VisibleBlock> result = new ArrayList<>();

        // 重要ブロックは最大30件
        int importantCount = Math.min(importantBlocks.size(), MAX_IMPORTANT_BLOCKS);
        for (int i = 0; i < importantCount; i++) {
            result.add(importantBlocks.get(i));
        }

        // 通常ブロックは最大20件（STONEなど）
        int normalCount = Math.min(normalBlocks.size(), MAX_NORMAL_BLOCKS);
        for (int i = 0; i < normalCount; i++) {
            result.add(normalBlocks.get(i));
        }

        return result;
    }

    /**
     * 周囲のドロップアイテムをスキャン
     */
    private List<VisibleEntity> scanItems(Location center, int radius) {
        List<VisibleEntity> items = new ArrayList<>();
        World world = center.getWorld();

        // 周囲のエンティティを取得
        for (Entity entity : world.getNearbyEntities(center, radius, verticalScanRange, radius)) {
            if (!(entity instanceof Item)) {
                continue;
            }

            Item item = (Item) entity;
            ItemStack stack = item.getItemStack();
            Location loc = item.getLocation();

            double distance = center.distance(loc);

            VisibleEntity visibleItem = new VisibleEntity(
                new Position(loc.getX(), loc.getY(), loc.getZ()),
                "ITEM",
                stack.getType().toString(),
                stack.getAmount(),
                Math.round(distance * 100.0) / 100.0
            );

            items.add(visibleItem);
        }

        // 距離でソート
        items.sort(Comparator.comparingDouble(VisibleEntity::getDistance));

        return items;
    }

    /**
     * 周囲のプレイヤーをスキャン
     */
    private List<VisibleEntity> scanPlayers(Location center, int radius) {
        List<VisibleEntity> players = new ArrayList<>();
        World world = center.getWorld();

        for (Entity entity : world.getNearbyEntities(center, radius, verticalScanRange, radius)) {
            if (!(entity instanceof Player)) {
                continue;
            }

            Player player = (Player) entity;
            Location loc = player.getLocation();

            double distance = center.distance(loc);

            VisibleEntity visiblePlayer = new VisibleEntity(
                new Position(loc.getX(), loc.getY(), loc.getZ()),
                "PLAYER",
                player.getName(),
                1,
                Math.round(distance * 100.0) / 100.0
            );

            players.add(visiblePlayer);
        }

        // 距離でソート
        players.sort(Comparator.comparingDouble(VisibleEntity::getDistance));

        return players;
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

        // 優先順位: 足(piglin) -> 見た目(armor stand) -> マーカー類
        Location loc = findFirstByTag(world, BOT_FEET_TAG);
        if (loc != null) {
            return loc;
        }

        loc = findFirstByTag(world, BOT_BODY_TAG);
        if (loc != null) {
            return loc;
        }

        loc = findFirstByTag(world, MOVE_MARKER_TAG);
        if (loc != null) {
            return loc;
        }

        loc = findFirstByTag(world, WOOD_MARKER_TAG);
        if (loc != null) {
            return loc;
        }

        loc = findFirstByTag(world, STONE_MARKER_TAG);
        if (loc != null) {
            return loc;
        }

        plugin.getLogger().fine("Bot entity not found in world (tags: test1/rider1/aim1/aim1o/aim1s)");
        return null;
    }

    private Location findFirstByTag(World world, String tag) {
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(tag)) {
                return entity.getLocation();
            }
        }
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
