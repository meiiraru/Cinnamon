package cinnamon.model.gltf;

import java.util.HashMap;
import java.util.Map;

public class Primitive {
    private int
            indices = -1,
            material = -1,
            mode = -1;
    private final Map<String, Integer> attributes = new HashMap<>();

    public int getIndices() {
        return indices;
    }

    public void setIndices(int indices) {
        this.indices = indices;
    }

    public int getMaterial() {
        return material;
    }

    public void setMaterial(int material) {
        this.material = material;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public Map<String, Integer> getAttributes() {
        return attributes;
    }
}
