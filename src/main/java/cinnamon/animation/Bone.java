package cinnamon.animation;

import cinnamon.model.ModelTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Bone {

    private final String name;
    private final boolean isModel;

    private final List<Bone> children = new ArrayList<>();
    private final ModelTransform transform = new ModelTransform();

    public Bone(String name) {
        this(name, false);
    }

    public Bone(String name, boolean model) {
        this.name = name;
        this.isModel = model;
    }

    public Bone(Bone other, Map<Bone, Bone> boneMap) {
        this.name = other.name;
        this.isModel = other.isModel;
        this.transform.setPivot(other.transform.getPivot());
        this.transform.setPivotRot(other.transform.getPivotRot());

        boneMap.put(other, this);

        for (Bone child : other.children)
            children.add(new Bone(child, boneMap));
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

    public ModelTransform getTransform() {
        return transform;
    }
}
