package plugin.midorin.info.aIminer.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import plugin.midorin.info.aIminer.brain.BrainFileManager;

import java.util.logging.Logger;

public class ChatListener implements Listener {
    private final BrainFileManager brainManager;
    private final Logger logger;

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

        // デバッグログ
        logger.info(String.format("Chat captured: [%s] %s", player, message));
    }
}
