package plugin.midorin.info.aIminer.model;

import java.util.HashMap;
import java.util.Map;

public class Task {
    private int id;
    private TaskType type;
    private Map<String, Object> parameters;
    private String reason;
    private TaskStatus status;
    private long createdAt;

    public Task() {
        this.parameters = new HashMap<>();
        this.status = TaskStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public Task(int id, TaskType type, Map<String, Object> parameters, String reason) {
        this.id = id;
        this.type = type;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.reason = reason;
        this.status = TaskStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
