package cinnamon.model.gltf;

import java.util.ArrayList;
import java.util.List;

public class Scene {

    private String name = "";
    private final List<Node> nodes = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Node> getNodes() {
        return nodes;
    }
}
