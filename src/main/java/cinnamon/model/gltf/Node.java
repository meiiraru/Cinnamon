package cinnamon.model.gltf;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private String name = "";

    private int mesh = -1;
    private final List<Node> children = new ArrayList<>();

    private final Vector3f translation = new Vector3f();
    private final Vector3f scale = new Vector3f(1, 1, 1);
    private final Quaternionf rotation = new Quaternionf();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMeshIndex(int mesh) {
        this.mesh = mesh;
    }

    public int getMeshIndex() {
        return mesh;
    }

    public boolean isMesh() {
        return mesh != -1;
    }

    public List<Node> getChildren() {
        return children;
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public Vector3f getScale() {
        return scale;
    }

    public Quaternionf getRotation() {
        return rotation;
    }
}
