package plugin.midorin.info.aIminer.model;

import java.util.ArrayList;
import java.util.List;

public class BlockVisionData {
    private int viewDistance;
    private ViewDirection viewDirection;
    private List<VisibleBlock> visibleBlocks;
    private List<VisibleEntity> nearbyItems;      // ドロップアイテム
    private List<VisibleEntity> nearbyPlayers;    // 近くのプレイヤー
    private Position botPosition;                  // ボットの現在位置

    public BlockVisionData() {
        this.viewDistance = 10;
        this.viewDirection = new ViewDirection();
        this.visibleBlocks = new ArrayList<>();
        this.nearbyItems = new ArrayList<>();
        this.nearbyPlayers = new ArrayList<>();
        this.botPosition = null;
    }

    public BlockVisionData(int viewDistance, ViewDirection viewDirection, List<VisibleBlock> visibleBlocks) {
        this.viewDistance = viewDistance;
        this.viewDirection = viewDirection;
        this.visibleBlocks = visibleBlocks != null ? visibleBlocks : new ArrayList<>();
        this.nearbyItems = new ArrayList<>();
        this.nearbyPlayers = new ArrayList<>();
        this.botPosition = null;
    }

    // Getters and Setters
    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    public ViewDirection getViewDirection() {
        return viewDirection;
    }

    public void setViewDirection(ViewDirection viewDirection) {
        this.viewDirection = viewDirection;
    }

    public List<VisibleBlock> getVisibleBlocks() {
        return visibleBlocks;
    }

    public void setVisibleBlocks(List<VisibleBlock> visibleBlocks) {
        this.visibleBlocks = visibleBlocks;
    }

    public List<VisibleEntity> getNearbyItems() {
        return nearbyItems;
    }

    public void setNearbyItems(List<VisibleEntity> nearbyItems) {
        this.nearbyItems = nearbyItems;
    }

    public List<VisibleEntity> getNearbyPlayers() {
        return nearbyPlayers;
    }

    public void setNearbyPlayers(List<VisibleEntity> nearbyPlayers) {
        this.nearbyPlayers = nearbyPlayers;
    }

    public Position getBotPosition() {
        return botPosition;
    }

    public void setBotPosition(Position botPosition) {
        this.botPosition = botPosition;
    }
}
