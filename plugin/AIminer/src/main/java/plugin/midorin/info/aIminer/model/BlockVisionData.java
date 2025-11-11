package plugin.midorin.info.aIminer.model;

import java.util.ArrayList;
import java.util.List;

public class BlockVisionData {
    private int viewDistance;
    private ViewDirection viewDirection;
    private List<VisibleBlock> visibleBlocks;

    public BlockVisionData() {
        this.viewDistance = 10;
        this.viewDirection = new ViewDirection();
        this.visibleBlocks = new ArrayList<>();
    }

    public BlockVisionData(int viewDistance, ViewDirection viewDirection, List<VisibleBlock> visibleBlocks) {
        this.viewDistance = viewDistance;
        this.viewDirection = viewDirection;
        this.visibleBlocks = visibleBlocks != null ? visibleBlocks : new ArrayList<>();
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
}
