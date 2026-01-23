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
    private final Map<String, Animation> animations;

    public AnimatedObjRenderer(AnimatedObjRenderer other) {
        super(other);
        Map<Bone, Bone> boneMap = new HashMap<>();
        this.bone = new Bone(other.bone, boneMap);

        this.animations = new HashMap<>();
        for (Animation animation : other.animations.values())
            this.animations.put(animation.getName(), new Animation(animation, boneMap));
    }

    public AnimatedObjRenderer(Mesh mesh, Pair<Bone, List<Animation>> animations) {
        super(mesh);
        this.bone = animations.first();
        this.animations = new HashMap<>();
        for (Animation animation : animations.second())
            this.animations.put(animation.getName(), animation);
    }

    @Override
    public void render(MatrixStack matrices, Material material) {
        if (isFreed())
            return;

        //tick animations
        for (Animation animation : animations.values())
            animation.update();

        //render bone tree
        renderBone(bone, matrices, s -> renderMesh(meshes.get(s), material));
    }

    @Override
    public void renderWithoutMaterial(MatrixStack matrices) {
        if (isFreed())
            return;

        //tick animations
        for (Animation animation : animations.values())
            animation.update();

        //render bone tree
        renderBone(bone, matrices, s -> renderMeshWithoutMaterial(meshes.get(s)));
    }

    private void renderBone(Bone bone, MatrixStack matrices, Consumer<String> renderFunction) {
        if (bone.isModel()) {
            renderFunction.accept(bone.getName());
            return;
        }

        if (bone.getChildren().isEmpty())
            return;

        matrices.pushMatrix();
        bone.getTransform().applyTransform(matrices);
        Shader.activeShader.applyMatrixStack(matrices);

        for (Bone child : bone.getChildren())
            renderBone(child, matrices, renderFunction);

        matrices.popMatrix();
    }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public List<String> getAnimations() {
        return new ArrayList<>(animations.keySet());
    }

    public void stopAllAnimations() {
        for (Animation animation : animations.values())
            animation.stop();
    }

    public Bone getBone() {
        return bone;
    }
}
