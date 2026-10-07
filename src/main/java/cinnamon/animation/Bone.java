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

    private Bone parent;

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

        for (Bone child : other.children) {
            Bone newChild = new Bone(child, boneMap);
            children.add(newChild);
            newChild.setParent(this);
        }
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

    public Bone getParent() {
        return parent;
    }

    public void setParent(Bone parent) {
        this.parent = parent;
    }

    public ModelTransform getTransform() {
        return transform;
    }

    public Bone findBone(String name) {
        if (this.name.equals(name))
            return this;

        for (Bone child : children) {
            Bone found = child.findBone(name);
            if (found != null)
                return found;
        }

        return null;
    }
}
