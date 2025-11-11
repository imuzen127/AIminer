package plugin.midorin.info.aIminer.model;

import java.util.ArrayList;
import java.util.List;

public class VisionData {
    private List<ChatMessage> chat;
    private BlockVisionData blocks;

    public VisionData() {
        this.chat = new ArrayList<>();
        this.blocks = new BlockVisionData();
    }

    public VisionData(List<ChatMessage> chat, BlockVisionData blocks) {
        this.chat = chat != null ? chat : new ArrayList<>();
        this.blocks = blocks != null ? blocks : new BlockVisionData();
    }

    // Getters and Setters
    public List<ChatMessage> getChat() {
        return chat;
    }

    public void setChat(List<ChatMessage> chat) {
        this.chat = chat;
    }

    public BlockVisionData getBlocks() {
        return blocks;
    }

    public void setBlocks(BlockVisionData blocks) {
        this.blocks = blocks;
    }
}
