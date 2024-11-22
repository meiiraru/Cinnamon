package cinnamon.render.model;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.model.material.Material;
import cinnamon.model.obj.Mesh;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AnimatedObjRenderer extends ObjRenderer {

    private final Bone bone;
    private final List<Animation> animations;

    public AnimatedObjRenderer(AnimatedObjRenderer other) {
        super(other);
        Map<Bone, Bone> boneMap = new HashMap<>();
        this.bone = new Bone(other.bone, boneMap);

        this.animations = new ArrayList<>();
        for (Animation animation : other.animations)
            this.animations.add(new Animation(animation, boneMap));
    }

    public AnimatedObjRenderer(Mesh mesh, Pair<Bone, List<Animation>> animations) {
        super(mesh);
        this.bone = animations.first();
        this.animations = animations.second();
    }

    @Override
    public void render(MatrixStack matrices, Material material) {
        //tick animations
        for (Animation animation : animations)
            animation.update();

        //render bone tree
        renderBone(bone, matrices, s -> renderGroup(s, material));
    }

    @Override
    public void renderWithoutMaterial(MatrixStack matrices) {
        //tick animations
        for (Animation animation : animations)
            animation.update();

        //render bone tree
        renderBone(bone, matrices, this::renderGroupWithoutMaterial);
    }

    private void renderBone(Bone bone, MatrixStack matrices, Consumer<String> renderFunction) {
        if (bone.isModel()) {
            renderFunction.accept(bone.getName());
            return;
        }

        if (bone.getChildren().isEmpty())
            return;

        matrices.push();
        bone.getTransform().applyTransform(matrices);
        Shader.activeShader.applyMatrixStack(matrices);

        for (Bone child : bone.getChildren())
            renderBone(child, matrices, renderFunction);

        matrices.pop();
    }

    public Animation getAnimation(String name) {
        for (Animation animation : animations) {
            if (animation.getName().equals(name))
                return animation;
        }

        return null;
    }
}
