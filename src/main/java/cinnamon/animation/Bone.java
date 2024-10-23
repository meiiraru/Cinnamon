package cinnamon.animation;

import cinnamon.model.Transform;
import cinnamon.render.MatrixStack;
import cinnamon.render.Model;
import cinnamon.render.shader.Shader;

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

    public void render(Model model, MatrixStack matrices) {
        if (isModel) {
            model.renderGroup(name);
            return;
        }

        if (children.isEmpty())
            return;

        matrices.push();
        transform.applyTransform(matrices);
        Shader.activeShader.applyMatrixStack(matrices);

        for (Bone child : children)
            child.render(model, matrices);

        matrices.pop();
    }

    public String getName() {
        return name;
    }

    public List<Bone> getChildren() {
        return children;
    }

    public Transform getTransform() {
        return transform;
    }
}
