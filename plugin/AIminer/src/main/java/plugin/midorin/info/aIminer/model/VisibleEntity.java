package plugin.midorin.info.aIminer.model;

/**
 * 視界内のエンティティ（ドロップアイテム、プレイヤー等）を表すクラス
 */
public class VisibleEntity {
    private Position worldPosition;
    private String entityType;  // "ITEM", "PLAYER", etc.
    private String name;        // アイテム名やプレイヤー名
    private int count;          // アイテムの場合のスタック数
    private double distance;

    public VisibleEntity() {
    }

    public VisibleEntity(Position worldPosition, String entityType, String name, int count, double distance) {
        this.worldPosition = worldPosition;
        this.entityType = entityType;
        this.name = name;
        this.count = count;
        this.distance = distance;
    }

    // Getters and Setters
    public Position getWorldPosition() {
        return worldPosition;
    }

    public void setWorldPosition(Position worldPosition) {
        this.worldPosition = worldPosition;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
