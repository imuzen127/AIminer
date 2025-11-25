package plugin.midorin.info.aIminer.ai;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import plugin.midorin.info.aIminer.bot.BotManager;
import plugin.midorin.info.aIminer.brain.BrainFileManager;
import plugin.midorin.info.aIminer.model.BrainData;

/**
 * Periodic task to process brain data through AI server
 */
public class AIProcessingTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final BrainFileManager brainFileManager;
    private final BotManager botManager;
    private final AIServerClient aiClient;
    private final int processingIntervalSeconds;
    private static final long SKIP_LOG_COOLDOWN_MS = 15000L;

    // Processing interval in seconds (longer to allow LLM to complete)
    // Flag to prevent concurrent processing
    private volatile boolean isProcessing = false;
    private volatile boolean pendingImmediateRun = false;
    private long lastSkipLogMs = 0L;

    public AIProcessingTask(
            JavaPlugin plugin,
            BrainFileManager brainFileManager,
            BotManager botManager,
            String aiServerUrl,
            int processingIntervalSeconds,
            int timeoutSeconds
    ) {
        this.plugin = plugin;
        this.brainFileManager = brainFileManager;
        this.botManager = botManager;
        this.aiClient = new AIServerClient(aiServerUrl, plugin.getLogger(), timeoutSeconds);
        this.processingIntervalSeconds = Math.max(5, processingIntervalSeconds);
    }

    @Override
    public void run() {
        // Skip if bot is not summoned
        if (!botManager.isBotSummoned()) {
            // 毎回表示すると煩いので10回に1回だけ表示
            if (System.currentTimeMillis() % 100000 < 10000) {
                plugin.getLogger().info("AI processing waiting: Bot not summoned (use /bot start)");
            }
            return;
        }

        // Skip if already processing
        if (isProcessing) {
            long now = System.currentTimeMillis();
            if (now - lastSkipLogMs > SKIP_LOG_COOLDOWN_MS) {
                plugin.getLogger().info("AI processing already running; will queue another run after it finishes");
                lastSkipLogMs = now;
            }
            pendingImmediateRun = true; // ensure one more run happens right after current one
            return;
        }

        plugin.getLogger().info("AI processing cycle triggered (every " + processingIntervalSeconds + "s)");
        lastSkipLogMs = 0L; // reset so future skips can log after cooldown

        // Run AI processing asynchronously to avoid blocking server
        isProcessing = true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                processWithQueue();
            } finally {
                isProcessing = false;
            }
        });
    }

    /**
     * Process brain data through AI server
     */
    private void processWithAI() {
        plugin.getLogger().info("Starting AI brain processing...");

        // Load current brain state
        BrainData currentBrain = brainFileManager.getBrainData();

        // Send to AI server
        BrainData updatedBrain = aiClient.processBrain(currentBrain);

        if (updatedBrain == null) {
            plugin.getLogger().warning("AI processing failed - brain state not updated");
            return;
        }

        // Update brain file manager with new data
        brainFileManager.setBrainData(updatedBrain);
        brainFileManager.saveBrainFile();

        plugin.getLogger().info("AI processing completed and brain state updated");
    }

    /**
     * Process brain data with a simple queue: if a run was requested while another
     * was in-flight, run once more immediately after finishing.
     */
    private void processWithQueue() {
        boolean runAgain;
        do {
            processWithAI();
            runAgain = pendingImmediateRun;
            pendingImmediateRun = false;
        } while (runAgain);
    }

    /**
     * Start the AI processing loop
     */
    public void startProcessingLoop() {
        // Check server health before starting
        plugin.getLogger().info("Checking AI server health...");
        if (aiClient.checkHealth()) {
            plugin.getLogger().info("AI server is ready!");
        } else {
            plugin.getLogger().warning(
                    "AI server health check failed. Processing will continue, " +
                    "but requests may fail until server is available."
            );
        }

        // Start periodic task (every N seconds)
        long intervalTicks = processingIntervalSeconds * 20L;
        this.runTaskTimer(plugin, 100L, intervalTicks); // Start after 5 seconds, then every N seconds

        plugin.getLogger().info(String.format(
                "AI processing task started (interval: %d seconds)",
                processingIntervalSeconds
        ));
    }

    /**
     * Stop the AI processing loop
     */
    public void stopProcessingLoop() {
        this.cancel();
        plugin.getLogger().info("AI processing task stopped");
    }

    /**
     * Trigger an immediate AI processing (can be called by command)
     */
    public void triggerImmediateProcessing() {
        if (isProcessing) {
            plugin.getLogger().info("AI processing already in progress");
            return;
        }

        plugin.getLogger().info("Triggering immediate AI processing...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            isProcessing = true;
            try {
                processWithAI();
            } finally {
                isProcessing = false;
            }
        });
    }
}
