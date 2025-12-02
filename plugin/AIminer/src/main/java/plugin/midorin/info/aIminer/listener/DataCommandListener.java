package plugin.midorin.info.aIminer.listener;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.util.CommandResultCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * data getコマンドの結果をキャプチャするリスナー
 * サーバーログからNBTデータを抽出
 */
public class DataCommandListener {
    private final JavaPlugin plugin;
    private final ConcurrentLinkedQueue<String> inventoryResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> itemPosResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> itemTypeResults = new ConcurrentLinkedQueue<>();

    private volatile boolean capturingInventory = false;
    private volatile boolean capturingItemPos = false;
    private volatile boolean capturingItemType = false;

    // 最新のキャプチャ結果
    private volatile List<CommandResultCapture.InventoryItem> lastInventory = new ArrayList<>();
    private volatile double[] lastItemPos = null;
    private volatile CommandResultCapture.InventoryItem lastItemType = null;

    private Handler logHandler;

    public DataCommandListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * ログハンドラを登録してコマンド結果をキャプチャ
     */
    public void register() {
        Logger serverLogger = plugin.getServer().getLogger();

        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null || record.getMessage() == null) return;

                String message = record.getMessage();

                // "has the following entity data:" を含むログをキャプチャ
                if (message.contains("has the following entity data:")) {
                    processDataCommandResult(message);
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };

        serverLogger.addHandler(logHandler);
        plugin.getLogger().info("DataCommandListener registered");
    }

    /**
     * ログハンドラを解除
     */
    public void unregister() {
        if (logHandler != null) {
            plugin.getServer().getLogger().removeHandler(logHandler);
            plugin.getLogger().info("DataCommandListener unregistered");
        }
    }

    /**
     * data getコマンドの結果を処理
     */
    private void processDataCommandResult(String message) {
        // インベントリキャプチャ中
        if (capturingInventory) {
            List<CommandResultCapture.InventoryItem> items =
                CommandResultCapture.parseInventoryFromLog(message);
            if (!items.isEmpty()) {
                lastInventory = items;
                inventoryResults.offer(message);
            }
            capturingInventory = false;
        }

        // アイテム座標キャプチャ中
        if (capturingItemPos) {
            double[] pos = CommandResultCapture.parsePositionFromLog(message);
            if (pos != null) {
                lastItemPos = pos;
                itemPosResults.offer(message);
            }
            capturingItemPos = false;
        }

        // アイテムタイプキャプチャ中
        if (capturingItemType) {
            CommandResultCapture.InventoryItem item =
                CommandResultCapture.parseItemFromLog(message);
            if (item != null) {
                lastItemType = item;
                itemTypeResults.offer(message);
            }
            capturingItemType = false;
        }
    }

    /**
     * インベントリ取得コマンドを実行し、結果を待つ
     * カスタムNBT: data.Inventory にアクセス
     */
    public List<CommandResultCapture.InventoryItem> captureInventory(String botTag) {
        lastInventory = new ArrayList<>();
        capturingInventory = true;

        // カスタムNBTのdata.Inventoryにアクセス
        String command = String.format("data get entity @e[tag=%s,limit=1] data.Inventory", botTag);
        org.bukkit.Bukkit.dispatchCommand(
            org.bukkit.Bukkit.getConsoleSender(), command
        );

        // 少し待ってから結果を返す（同期的な待機は避けたいが、簡易実装）
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        capturingInventory = false;
        return new ArrayList<>(lastInventory);
    }

    /**
     * 近くのアイテムの座標を取得
     */
    public double[] captureNearbyItemPos(String botTag) {
        lastItemPos = null;
        capturingItemPos = true;

        String command = String.format(
            "execute as @e[tag=%s,limit=1] run data get entity @n[type=item] Pos", botTag
        );
        org.bukkit.Bukkit.dispatchCommand(
            org.bukkit.Bukkit.getConsoleSender(), command
        );

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        capturingItemPos = false;
        return lastItemPos;
    }

    /**
     * 近くのアイテムのタイプを取得
     */
    public CommandResultCapture.InventoryItem captureNearbyItemType(String botTag) {
        lastItemType = null;
        capturingItemType = true;

        String command = String.format(
            "execute as @e[tag=%s,limit=1] run data get entity @n[type=item] Item", botTag
        );
        org.bukkit.Bukkit.dispatchCommand(
            org.bukkit.Bukkit.getConsoleSender(), command
        );

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        capturingItemType = false;
        return lastItemType;
    }

    /**
     * 近くのアイテム情報（座標とタイプ）を取得
     */
    public CommandResultCapture.NearbyItem captureNearbyItem(String botTag) {
        double[] pos = captureNearbyItemPos(botTag);
        if (pos == null) {
            return null;
        }

        CommandResultCapture.InventoryItem item = captureNearbyItemType(botTag);
        if (item == null) {
            return null;
        }

        return new CommandResultCapture.NearbyItem(
            item.getItemId(), item.getCount(),
            pos[0], pos[1], pos[2]
        );
    }

    /**
     * 最後に取得したインベントリを返す
     */
    public List<CommandResultCapture.InventoryItem> getLastInventory() {
        return new ArrayList<>(lastInventory);
    }
}
