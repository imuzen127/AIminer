package plugin.midorin.info.aIminer.model;

import java.util.HashMap;
import java.util.Map;

public class Memory {
    private Map<String, Object> data;

    public Memory() {
        this.data = new HashMap<>();
        initializeDefaultStructure();
    }

    private void initializeDefaultStructure() {
        this.data.put("important_locations", new java.util.ArrayList<>());
        this.data.put("player_requests", new java.util.ArrayList<>());
        this.data.put("inventory_state", new HashMap<String, Object>());
        this.data.put("current_position", new Position(0, 0, 0));
    }

    // Getters and Setters
    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void put(String key, Object value) {
        this.data.put(key, value);
    }

    public Object get(String key) {
        return this.data.get(key);
    }
}
