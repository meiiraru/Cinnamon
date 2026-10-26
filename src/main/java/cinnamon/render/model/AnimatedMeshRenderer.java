package cinnamon.render.model;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.math.collision.shape.OBB;
import cinnamon.model.material.Material;
import cinnamon.model.mesh.Mesh;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AnimatedMeshRenderer extends MeshRenderer {

    private final Bone bone;
    private final Map<String, Animation> animations;

    private final Map<String, OBB> obbCache = new HashMap<>();
    private boolean updateOBBCache = false;

    public AnimatedMeshRenderer(AnimatedMeshRenderer other) {
        super(other);
        Map<Bone, Bone> boneMap = new HashMap<>();
        this.bone = new Bone(other.bone, boneMap);

        this.animations = new HashMap<>();
        for (Animation animation : other.animations.values())
            this.animations.put(animation.getName(), new Animation(animation, boneMap));

        for (String meshName : other.meshes.keySet())
            obbCache.put(meshName, new OBB(other.obbCache.get(meshName)));
        this.updateOBBCache = other.updateOBBCache;
    }

    public AnimatedMeshRenderer(Mesh mesh) {
        super(mesh);

        this.bone = mesh.getAnimationData().first();
        this.animations = new HashMap<>();
        for (Animation animation : mesh.getAnimationData().second())
            this.animations.put(animation.getName(), animation);

        for (String meshName : this.meshes.keySet())
            obbCache.put(meshName, new OBB(this.meshes.get(meshName).getAABB()));
    }

    @Override
    public void render(MatrixStack matrices, Material material) {
        if (isFreed())
            return;

        //tick animations
        for (Animation animation : animations.values())
            animation.update();

        //render bone tree
        renderBone(bone, matrices, s -> {
            MeshData mesh = meshes.get(s);
            if (mesh != null)
                renderMesh(mesh, material);
        });
    }

    @Override
    public void renderWithoutMaterial(MatrixStack matrices) {
        if (isFreed())
            return;

        //tick animations
        for (Animation animation : animations.values())
            animation.update();

        //render bone tree
        renderBone(bone, matrices, s -> {
            MeshData mesh = meshes.get(s);
            if (mesh != null)
                renderMeshWithoutMaterial(mesh);
        });
    }

    private void renderBone(Bone bone, MatrixStack matrices, Consumer<String> renderFunction) {
        if (bone.isModel()) {
            renderFunction.accept(bone.getName());
            return;
        }

        matrices.pushMatrix();

        this.updateOBBCache |= bone.getTransform().isDirty();
        bone.getTransform().applyTransform(matrices);

        if (bone.getChildren().isEmpty()) {
            matrices.popMatrix();
            return;
        }

        Shader.activeShader.applyMatrixStack(matrices);

        for (Bone child : bone.getChildren())
            renderBone(child, matrices, renderFunction);

        matrices.popMatrix();
        Shader.activeShader.applyMatrixStack(matrices);
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

    @Override
    public List<OBB> getGroupsOBB() {
        if (updateOBBCache) {
            updateOBBTransforms(bone, new MatrixStack());
            updateOBBCache = false;
        }
        return new ArrayList<>(obbCache.values());
    }

    protected void updateOBBTransforms(Bone bone, MatrixStack matrices) {
        matrices.pushMatrix();
        bone.getTransform().applyTransform(matrices);

        for (Bone child : bone.getChildren()) {
            if (child.isModel()) {
                String key = child.getName();
                MeshData mesh = meshes.get(key);
                if (mesh != null) {
                    OBB obb = obbCache.get(key);
                    obb.set(mesh.getAABB());
                    obb.applyMatrix(matrices.peek().pos());
                    obbCache.put(key, obb);
                }
            } else {
                updateOBBTransforms(child, matrices);
            }
        }

        matrices.popMatrix();
    }
}
