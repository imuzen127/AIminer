package plugin.midorin.info.aIminer;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.listener.ChatListener;

public final class AIminer extends JavaPlugin {

    private BrainFileManager brainFileManager;

    @Override
    public void onEnable() {
        // プラグイン起動ログ
        getLogger().info("AIminer plugin is starting...");

        // 脳ファイルマネージャーの初期化
        brainFileManager = new BrainFileManager(getDataFolder());
        brainFileManager.loadBrainFile();

        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(
            new ChatListener(brainFileManager),
            this
        );

        getLogger().info("AIminer plugin has been enabled!");
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
}
