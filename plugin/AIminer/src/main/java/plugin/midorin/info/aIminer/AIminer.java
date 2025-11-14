package plugin.midorin.info.aIminer;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.command.BotCommand;
import plugin.midorin.info.aIminer.executor.TaskExecutor;
import plugin.midorin.info.aIminer.listener.ChatListener;

public final class AIminer extends JavaPlugin {

    private BrainFileManager brainFileManager;
    private BotManager botManager;
    private TaskExecutor taskExecutor;

    @Override
    public void onEnable() {
        // プラグイン起動ログ
        getLogger().info("AIminer plugin is starting...");

        // 脳ファイルマネージャーの初期化
        brainFileManager = new BrainFileManager(getDataFolder());
        brainFileManager.loadBrainFile();

        // ボットマネージャーの初期化
        botManager = new BotManager(this);

        // タスク実行システムの初期化と起動
        taskExecutor = new TaskExecutor(this, brainFileManager);
        taskExecutor.startTaskLoop();
        getLogger().info("Task executor started.");

        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(
            new ChatListener(brainFileManager),
            this
        );

        // コマンドの登録
        getCommand("bot").setExecutor(new BotCommand(botManager, brainFileManager));
        getLogger().info("Bot command registered.");

        getLogger().info("AIminer plugin has been enabled!");
        getLogger().info("Use /bot start to begin!");
    }

    @Override
    public void onDisable() {
        // 脳ファイルを保存
        if (brainFileManager != null) {
            brainFileManager.saveBrainFile();
            getLogger().info("Brain file saved.");
        }

        getLogger().info("AIminer plugin has been disabled!");
    }

    public BrainFileManager getBrainFileManager() {
        return brainFileManager;
    }

    public BotManager getBotManager() {
        return botManager;
    }
}
