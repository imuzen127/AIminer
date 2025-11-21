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
        this.description = "あなたはMinecraft内で動作するAIボットです。プレイヤーと協力してタスクを実行します。" +
                "あなたができることは限られています：木を掘る、石を掘る、移動する、チャットで発言する、情報を取得する。" +
                "プレイヤーの発言に応答し、適切なタスクを生成してください。";
        this.visionRules = "視覚情報（チャット履歴、周囲のブロック）は自動的に更新されます。" +
                "プレイヤーのチャット発言は自動で記録されます。" +
                "重要な情報（プレイヤーからの依頼、発見した資源の場所など）はmemoryに保存してください。";
        this.memoryRules = "memoryには重要な情報のみを記録してください：" +
                "プレイヤーからの依頼内容、重要な座標、作業の進捗状況など。" +
                "不要になった情報は削除可能です。memoryは長期記憶として機能します。";
        this.taskRules = "tasksには次に実行したい行動を1つだけ記述してください。" +
                "プレイヤーから話しかけられたらCHATタスクで応答してください。" +
                "タスクは順番に実行され、完了するまで次のタスクは開始されません。" +
                "何もすることがない場合はnew_taskをnullにしてください。";
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
