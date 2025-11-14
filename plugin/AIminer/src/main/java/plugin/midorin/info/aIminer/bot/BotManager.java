package plugin.midorin.info.aIminer.bot;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * ボットの召喚と管理を行うクラス
 */
public class BotManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private boolean botSummoned = false;

    public BotManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * ボットを召喚
     */
    public boolean summonBot() {
        if (botSummoned) {
            logger.info("Bot is already summoned.");
            return true;
        }

        logger.info("Summoning bot...");

        // 見た目召喚
        boolean success1 = Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            "function imuzen127x74:summanekin"
        );

        // 少し待ってから足を召喚
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean success2 = Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "function imuzen127x74:sumpig"
            );

            if (success2) {
                botSummoned = true;
                logger.info("Bot summoned successfully!");
            } else {
                logger.warning("Failed to summon bot feet.");
            }
        }, 10L); // 0.5秒後に実行

        return success1;
    }

    /**
     * ボットが召喚されているかチェック
     */
    public boolean isBotSummoned() {
        return botSummoned;
    }

    /**
     * ボットの状態をリセット
     */
    public void resetBot() {
        botSummoned = false;
        logger.info("Bot status reset.");
    }
}
