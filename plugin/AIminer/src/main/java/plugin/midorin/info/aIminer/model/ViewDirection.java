package plugin.midorin.info.aIminer.model;

public class ViewDirection {
    private double yaw;
    private double pitch;

    public ViewDirection() {
    }

    public ViewDirection(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    // Getters and Setters
    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }
}
