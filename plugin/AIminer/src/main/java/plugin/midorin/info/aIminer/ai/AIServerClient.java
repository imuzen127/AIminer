package plugin.midorin.info.aIminer.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import plugin.midorin.info.aIminer.model.BrainData;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * HTTP Client for communicating with AI Brain API Server
 */
public class AIServerClient {
    private final String apiUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Logger logger;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public AIServerClient(String apiUrl, Logger logger) {
        this.apiUrl = apiUrl;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Configure HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Process brain data through AI server
     *
     * @param brainData Current brain state
     * @return Updated brain data with new task, or null if failed
     */
    public BrainData processBrain(BrainData brainData) {
        try {
            logger.info("Sending brain data to AI server: " + apiUrl);

            // Create request body
            BrainRequest request = new BrainRequest(brainData);
            String jsonBody = gson.toJson(request);

            RequestBody body = RequestBody.create(jsonBody, JSON);

            // Build HTTP request
            Request httpRequest = new Request.Builder()
                    .url(apiUrl + "/api/brain")
                    .post(body)
                    .build();

            // Execute request
            long startTime = System.currentTimeMillis();
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                long responseTime = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    logger.warning(String.format(
                            "AI server returned error: %d %s",
                            response.code(),
                            response.message()
                    ));
                    return null;
                }

                // Parse response
                String responseBody = response.body().string();
                BrainResponse brainResponse = gson.fromJson(responseBody, BrainResponse.class);

                logger.info(String.format(
                        "AI processing completed in %dms (server: %dms) - Task added: %s",
                        responseTime,
                        brainResponse.getProcessingTimeMs(),
                        brainResponse.isTaskAdded()
                ));

                if (brainResponse.isTaskAdded() && brainResponse.getTask() != null) {
                    logger.info(String.format(
                            "New task generated: %s (ID: %d)",
                            brainResponse.getTask().get("type"),
                            brainResponse.getTask().get("id")
                    ));
                }

                return brainResponse.getBrainData();
            }

        } catch (IOException e) {
            logger.severe("Failed to communicate with AI server: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.severe("Error processing brain data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if AI server is healthy
     *
     * @return true if server is responsive and healthy
     */
    public boolean checkHealth() {
        try {
            Request request = new Request.Builder()
                    .url(apiUrl + "/health")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("AI server health check: OK");
                    return true;
                } else {
                    logger.warning("AI server health check failed: " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            logger.warning("Cannot reach AI server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Request wrapper for brain processing
     */
    private static class BrainRequest {
        @SuppressWarnings("unused")
        private final BrainData brain_data;

        public BrainRequest(BrainData brainData) {
            this.brain_data = brainData;
        }
    }

    /**
     * Response wrapper from brain processing
     */
    private static class BrainResponse {
        private BrainData brain_data;
        private int processing_time_ms;
        private boolean task_added;
        private java.util.Map<String, Object> task;

        // Getters
        public BrainData getBrainData() {
            return brain_data;
        }

        public int getProcessingTimeMs() {
            return processing_time_ms;
        }

        public boolean isTaskAdded() {
            return task_added;
        }

        public java.util.Map<String, Object> getTask() {
            return task;
        }
    }
}
