package plugin.midorin.info.aIminer.model;

public class ChatMessage {
    private String timestamp;
    private String player;
    private String message;

    public ChatMessage() {
    }

    public ChatMessage(String timestamp, String player, String message) {
        this.timestamp = timestamp;
        this.player = player;
        this.message = message;
    }

    // Getters and Setters
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
