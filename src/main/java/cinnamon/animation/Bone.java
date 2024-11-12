package cinnamon.animation;

import cinnamon.model.Transform;

import java.util.ArrayList;
import java.util.List;

public class Bone {

    private final String name;
    private final boolean isModel;

    private final List<Bone> children = new ArrayList<>();
    private final Transform transform = new Transform();

    public Bone(String name) {
        this(name, false);
    }

    public Bone(String name, boolean model) {
        this.name = name;
        this.isModel = model;
    }

    public String getName() {
        return name;
    }

    public boolean isModel() {
        return isModel;
    }

    public List<Bone> getChildren() {
        return children;
    }

    public Transform getTransform() {
        return transform;
    }
}
