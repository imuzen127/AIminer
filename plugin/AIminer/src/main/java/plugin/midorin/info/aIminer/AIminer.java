package plugin.midorin.info.aIminer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.ai.AIProcessingTask;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.command.BotCommand;
import plugin.midorin.info.aIminer.executor.TaskExecutor;
import plugin.midorin.info.aIminer.listener.ChatListener;
import plugin.midorin.info.aIminer.listener.DataCommandListener;
import plugin.midorin.info.aIminer.listener.PlayerActivityListener;
import plugin.midorin.info.aIminer.vision.VisionUpdateTask;

public final class AIminer extends JavaPlugin {

    private BrainFileManager brainFileManager;
    private BotManager botManager;
    private TaskExecutor taskExecutor;
    private VisionUpdateTask visionUpdateTask;
    private AIProcessingTask aiProcessingTask;
    private DataCommandListener dataCommandListener;

    @Override
    public void onEnable() {
        // プラグイン起動ログ
        getLogger().info("AIminer plugin is starting...");

        // 設定ファイルの保存（初回起動時）
        saveDefaultConfig();

        // 脳ファイルマネージャーの初期化
        brainFileManager = new BrainFileManager(getDataFolder());
        brainFileManager.loadBrainFile();

        // ボットマネージャーの初期化
        botManager = new BotManager(this);

        // データコマンドリスナーの初期化と登録
        dataCommandListener = new DataCommandListener(this);
        dataCommandListener.register();

        // 設定値の読み込み
        int visionRadius = getConfig().getInt("vision.scan-radius", 10);
        int visionVerticalRange = getConfig().getInt("vision.vertical-range", 5);
        int visionIntervalSeconds = getConfig().getInt("vision.update-interval", 5);
        int aiProcessingIntervalSeconds = getConfig().getInt("ai-server.interval", 60);
        int aiTimeoutSeconds = getConfig().getInt("ai-server.timeout-seconds", 120);

        // タスク実行システムの初期化と起動
        taskExecutor = new TaskExecutor(this, brainFileManager, botManager, dataCommandListener);
        taskExecutor.startTaskLoop();
        getLogger().info("Task executor started.");

        // 視覚システムの初期化と起動
        visionUpdateTask = new VisionUpdateTask(
            this,
            brainFileManager,
            botManager,
            dataCommandListener,
            visionRadius,
            visionVerticalRange,
            visionIntervalSeconds
        );
        visionUpdateTask.startVisionLoop();
        getLogger().info("Vision update system started.");

        // AI処理システムの初期化と起動
        boolean aiEnabled = getConfig().getBoolean("ai-server.enabled", true);
        if (aiEnabled) {
            String aiServerUrl = getConfig().getString("ai-server.url", "http://localhost:8080");
            aiProcessingTask = new AIProcessingTask(
                this,
                brainFileManager,
                botManager,
                aiServerUrl,
                aiProcessingIntervalSeconds,
                aiTimeoutSeconds
            );
            aiProcessingTask.startProcessingLoop();
            getLogger().info("AI processing system started (server: " + aiServerUrl + ")");
        } else {
            getLogger().info("AI processing system is disabled in config");
        }

        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(
            new ChatListener(brainFileManager, getLogger()),
            this
        );
        getServer().getPluginManager().registerEvents(
            new PlayerActivityListener(brainFileManager, getLogger()),
            this
        );

        // コマンドの登録（Paper 1.21対応）
        // plugin.ymlで定義したコマンドは自動的に登録されるため、
        // 起動後にExecutorを設定
        Bukkit.getScheduler().runTask(this, () -> {
            org.bukkit.command.PluginCommand botCommand = getCommand("bot");
            if (botCommand != null) {
                BotCommand executor = new BotCommand(botManager, brainFileManager, aiProcessingTask);
                botCommand.setExecutor(executor);
                botCommand.setTabCompleter(executor);
                getLogger().info("Bot command registered.");
            } else {
                getLogger().warning("Failed to register bot command!");
            }
        });

        getLogger().info("AIminer plugin has been enabled!");
        getLogger().info("Use /bot start to begin!");
    }

    @Override
    public void onDisable() {
        // AI処理タスクを停止
        if (aiProcessingTask != null) {
            aiProcessingTask.stopProcessingLoop();
        }

        // 視覚更新タスクを停止
        if (visionUpdateTask != null) {
            visionUpdateTask.stopVisionLoop();
        }

        // データコマンドリスナーを解除
        if (dataCommandListener != null) {
            dataCommandListener.unregister();
        }

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
