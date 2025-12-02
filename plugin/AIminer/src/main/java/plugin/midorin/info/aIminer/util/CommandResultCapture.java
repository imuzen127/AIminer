package plugin.midorin.info.aIminer.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraftコマンドの結果をキャプチャするユーティリティ
 * NBTデータをパースしてJavaオブジェクトに変換
 */
public class CommandResultCapture {

    // インベントリアイテムのパターン: {id: "minecraft:oak_log", count: 2}
    private static final Pattern ITEM_PATTERN = Pattern.compile(
        "\\{id:\\s*\"minecraft:([^\"]+)\",\\s*count:\\s*(\\d+)"
    );

    // 座標のパターン: [1.5d, -58.0d, 2.3d] or [-24.5, -58.0, 1.2]
    private static final Pattern POS_PATTERN = Pattern.compile(
        "\\[(-?[\\d.]+)d?,\\s*(-?[\\d.]+)d?,\\s*(-?[\\d.]+)d?\\]"
    );

    /**
     * ボットのインベントリを取得
     * コマンド: /data get entity @e[tag=test1,limit=1] Inventory
     */
    public static List<InventoryItem> getBotInventory(String botTag) {
        List<InventoryItem> items = new ArrayList<>();

        // エンティティを探してNBTを直接読み取る
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(botTag)) {
                    // PersistentDataContainerやNBT APIを使用
                    // Paper/Spigotでは直接NBTにアクセスするのが難しいため
                    // コマンド実行→ログキャプチャの方法を使う

                    // コマンドを実行してログから結果を取得する代わりに
                    // Bukkitのdata commandの結果をキャプチャ
                    String command = String.format(
                        "data get entity @e[tag=%s,limit=1] Inventory", botTag
                    );

                    // サーバーログにコマンド結果が出力される
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                    // 注: 実際の結果取得はログリスナーで行う
                    break;
                }
            }
        }

        return items;
    }

    /**
     * ログ出力からインベントリアイテムをパース
     * 例: [{id: "minecraft:oak_log", count: 2}, {id: "minecraft:cobblestone", count: 64}]
     */
    public static List<InventoryItem> parseInventoryFromLog(String logLine) {
        List<InventoryItem> items = new ArrayList<>();

        Matcher matcher = ITEM_PATTERN.matcher(logLine);
        while (matcher.find()) {
            String itemId = matcher.group(1);  // oak_log
            int count = Integer.parseInt(matcher.group(2));
            items.add(new InventoryItem(itemId, count));
        }

        return items;
    }

    /**
     * ログ出力から座標をパース
     * 例: [-24.5d, -58.0d, 1.2d]
     */
    public static double[] parsePositionFromLog(String logLine) {
        Matcher matcher = POS_PATTERN.matcher(logLine);
        if (matcher.find()) {
            return new double[] {
                Double.parseDouble(matcher.group(1)),
                Double.parseDouble(matcher.group(2)),
                Double.parseDouble(matcher.group(3))
            };
        }
        return null;
    }

    /**
     * ログ出力からアイテムタイプをパース
     * 例: {id: "minecraft:oak_log", count: 1}
     */
    public static InventoryItem parseItemFromLog(String logLine) {
        Matcher matcher = ITEM_PATTERN.matcher(logLine);
        if (matcher.find()) {
            String itemId = matcher.group(1);
            int count = Integer.parseInt(matcher.group(2));
            return new InventoryItem(itemId, count);
        }
        return null;
    }

    /**
     * インベントリアイテムを表すクラス
     */
    public static class InventoryItem {
        private final String itemId;
        private final int count;

        public InventoryItem(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }

        public String getItemId() {
            return itemId;
        }

        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return itemId + " x" + count;
        }
    }

    /**
     * 近くのアイテムエンティティ情報
     */
    public static class NearbyItem {
        private final String itemId;
        private final int count;
        private final double x, y, z;

        public NearbyItem(String itemId, int count, double x, double y, double z) {
            this.itemId = itemId;
            this.count = count;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getItemId() {
            return itemId;
        }

        public int getCount() {
            return count;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        @Override
        public String toString() {
            return String.format("%s x%d at (%.1f, %.1f, %.1f)", itemId, count, x, y, z);
        }
    }
}
