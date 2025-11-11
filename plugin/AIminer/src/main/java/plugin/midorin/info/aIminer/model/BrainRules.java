package plugin.midorin.info.aIminer.model;

import java.util.ArrayList;
import java.util.List;

public class BrainRules {
    private String description;
    private String visionRules;
    private String memoryRules;
    private String taskRules;
    private List<String> availableTasks;

    public BrainRules() {
        this.description = "このファイルはボットの脳です。visionから情報を読み取り、必要な情報をmemoryに追加し、tasksに実行したい行動を記述してください。";
        this.visionRules = "visionは常に更新されます。重要な情報はmemoryに保存してください。";
        this.memoryRules = "memoryには重要な情報のみを記録してください。不要になった情報は削除可能です。";
        this.taskRules = "tasksには次に実行したい行動を記述してください。タスクは順番に実行されます。";
        this.availableTasks = new ArrayList<>();
        initializeAvailableTasks();
    }

    private void initializeAvailableTasks() {
        availableTasks.add("MINE_WOOD");
        availableTasks.add("MINE_STONE");
        availableTasks.add("MOVE_TO");
        availableTasks.add("GET_INVENTORY");
        availableTasks.add("GET_POSITION");
        availableTasks.add("GET_ENTITY_POSITION");
        availableTasks.add("CHAT");
        availableTasks.add("READ_MEMORY");
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisionRules() {
        return visionRules;
    }

    public void setVisionRules(String visionRules) {
        this.visionRules = visionRules;
    }

    public String getMemoryRules() {
        return memoryRules;
    }

    public void setMemoryRules(String memoryRules) {
        this.memoryRules = memoryRules;
    }

    public String getTaskRules() {
        return taskRules;
    }

    public void setTaskRules(String taskRules) {
        this.taskRules = taskRules;
    }

    public List<String> getAvailableTasks() {
        return availableTasks;
    }

    public void setAvailableTasks(List<String> availableTasks) {
        this.availableTasks = availableTasks;
    }
}
