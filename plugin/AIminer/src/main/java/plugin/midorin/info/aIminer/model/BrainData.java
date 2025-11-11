package plugin.midorin.info.aIminer.model;

import java.util.ArrayList;
import java.util.List;

public class BrainData {
    private BrainRules rules;
    private VisionData vision;
    private Memory memory;
    private List<Task> tasks;

    public BrainData() {
        this.rules = new BrainRules();
        this.vision = new VisionData();
        this.memory = new Memory();
        this.tasks = new ArrayList<>();
    }

    public BrainData(BrainRules rules, VisionData vision, Memory memory, List<Task> tasks) {
        this.rules = rules != null ? rules : new BrainRules();
        this.vision = vision != null ? vision : new VisionData();
        this.memory = memory != null ? memory : new Memory();
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    // Getters and Setters
    public BrainRules getRules() {
        return rules;
    }

    public void setRules(BrainRules rules) {
        this.rules = rules;
    }

    public VisionData getVision() {
        return vision;
    }

    public void setVision(VisionData vision) {
        this.vision = vision;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
