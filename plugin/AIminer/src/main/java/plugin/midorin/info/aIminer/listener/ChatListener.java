package plugin.midorin.info.aIminer.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import plugin.midorin.info.aIminer.brain.BrainFileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {
    private final BrainFileManager brainManager;
    private final Logger logger;
    private static final Pattern COORD_PATTERN = Pattern.compile("(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)");

    public ChatListener(BrainFileManager brainManager, Logger logger) {
        this.brainManager = brainManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        String player = event.getPlayer().getName();

        // PlainTextComponentSerializerを使用してすべてのComponent型に対応
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // 空メッセージは無視
        if (message.isEmpty()) {
            return;
        }

        long timestamp = System.currentTimeMillis();

        // 脳ファイルにチャットメッセージを追加
        brainManager.addChatMessage(player, message, timestamp);

        // チャットから座標っぽい記述を抽出し、メモリにヒントとして保存
        Matcher m = COORD_PATTERN.matcher(message);
        if (m.find()) {
            try {
                int x = Integer.parseInt(m.group(1));
                int y = Integer.parseInt(m.group(2));
                int z = Integer.parseInt(m.group(3));
                String hint = String.format("(%d, %d, %d) - PLAYER_HINT by %s: %s", x, y, z, player, message);

                Object locObj = brainManager.getBrainData().getMemory().get("important_locations");
                List<Object> locs = locObj instanceof List ? (List<Object>) locObj : new ArrayList<>();
                locs.add(hint);
                brainManager.updateMemory("important_locations", locs);

                Map<String, Object> hintMap = new java.util.HashMap<>();
                hintMap.put("x", x);
                hintMap.put("y", y);
                hintMap.put("z", z);
                hintMap.put("player", player);
                hintMap.put("message", message);
                hintMap.put("timestamp", timestamp);
                brainManager.updateMemory("last_coordinate_hint", hintMap);

                logger.info(String.format("Coordinate hint captured from chat: %s", hint));
            } catch (NumberFormatException ignored) {
                // ignore parsing issues, continue
            }
        }

        // デバッグログ
        logger.info(String.format("Chat captured: [%s] %s", player, message));
    }
}
