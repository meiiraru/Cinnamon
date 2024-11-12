package cinnamon.render;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.model.material.Material;
import cinnamon.model.obj.Mesh;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Pair;

import java.util.List;
import java.util.function.Consumer;

public class AnimatedModel extends MeshModel {

    private final Bone bone;
    private final List<Animation> animations;

    public AnimatedModel(Mesh mesh, Pair<Bone, List<Animation>> animations) {
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
