package plugin.midorin.info.aIminer.model;

public class VisibleBlock {
    private Position relativePosition;
    private Position worldPosition;
    private String blockType;
    private double distance;

    public VisibleBlock() {
    }

    public VisibleBlock(Position relativePosition, Position worldPosition, String blockType, double distance) {
        this.relativePosition = relativePosition;
        this.worldPosition = worldPosition;
        this.blockType = blockType;
        this.distance = distance;
    }

    // Getters and Setters
    public Position getRelativePosition() {
        return relativePosition;
    }

    public void setRelativePosition(Position relativePosition) {
        this.relativePosition = relativePosition;
    }

    public Position getWorldPosition() {
        return worldPosition;
    }

    public void setWorldPosition(Position worldPosition) {
        this.worldPosition = worldPosition;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
