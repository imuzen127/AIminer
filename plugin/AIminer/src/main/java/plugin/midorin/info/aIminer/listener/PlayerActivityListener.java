package plugin.midorin.info.aIminer.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import plugin.midorin.info.aIminer.brain.BrainFileManager;

import java.util.logging.Logger;

/**
 * プレイヤーの入退室を視覚情報として記録する
 */
public class PlayerActivityListener implements Listener {
    private final BrainFileManager brainManager;
    private final Logger logger;

    public PlayerActivityListener(BrainFileManager brainManager, Logger logger) {
        this.brainManager = brainManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        long timestamp = System.currentTimeMillis();
        brainManager.addChatMessage("SYSTEM", player + " joined the world", timestamp);
        logger.fine("Join captured for " + player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        long timestamp = System.currentTimeMillis();
        brainManager.addChatMessage("SYSTEM", player + " left the world", timestamp);
        logger.fine("Quit captured for " + player);
    }
}
