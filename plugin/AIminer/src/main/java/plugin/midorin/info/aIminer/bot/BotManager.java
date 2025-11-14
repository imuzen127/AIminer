package plugin.midorin.info.aIminer.bot;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * ボットの召喚と管理を行うクラス
 */
public class BotManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private boolean botSummoned = false;
    private CommandSender botOwner = null;

    public BotManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * ボットを召喚（コマンド実行者の座標で召喚）
     */
    public boolean summonBot(CommandSender sender) {
        if (botSummoned) {
            logger.info("Bot is already summoned.");
            return true;
        }

        logger.info("Summoning bot at " + sender.getName() + "'s location...");

        // ボットのオーナーを記録
        botOwner = sender;

        // 見た目召喚（実行者の座標で召喚）
        boolean success1 = Bukkit.dispatchCommand(
            sender,
            "function imuzen127x74:summanekin"
        );

        // 少し待ってから足を召喚
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean success2 = Bukkit.dispatchCommand(
                sender,
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
        botOwner = null;
        logger.info("Bot status reset.");
    }

    /**
     * ボットのオーナー（召喚したプレイヤー）を取得
     * タスク実行時の座標コンテキストとして使用
     */
    public CommandSender getBotOwner() {
        // オーナーがオフラインの場合、オンラインの任意のプレイヤーを返す
        if (botOwner instanceof Player) {
            Player player = (Player) botOwner;
            if (!player.isOnline()) {
                // オンラインの最初のプレイヤーを取得
                return Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            }
        }
        return botOwner;
    }
}
