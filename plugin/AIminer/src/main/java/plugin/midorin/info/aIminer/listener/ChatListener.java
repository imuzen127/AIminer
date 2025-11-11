package plugin.midorin.info.aIminer.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import plugin.midorin.info.aIminer.brain.BrainFileManager;

public class ChatListener implements Listener {
    private final BrainFileManager brainManager;

    public ChatListener(BrainFileManager brainManager) {
        this.brainManager = brainManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncChatEvent event) {
        String player = event.getPlayer().getName();
        String message = "";

        // メッセージを文字列に変換
        if (event.message() instanceof TextComponent textComponent) {
            message = textComponent.content();
        }

        long timestamp = System.currentTimeMillis();

        // 脳ファイルにチャットメッセージを追加
        brainManager.addChatMessage(player, message, timestamp);
    }
}
