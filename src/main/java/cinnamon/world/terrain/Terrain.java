package cinnamon.world.terrain;

import cinnamon.animation.Animation;
import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Collider;
import cinnamon.math.collision.OBB;
import cinnamon.model.ModelManager;
import cinnamon.model.material.Material;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.Resource;
import cinnamon.world.Mask;
import cinnamon.world.WorldObject;
import cinnamon.world.entity.Entity;
import cinnamon.world.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Terrain extends WorldObject {

    protected final ModelRenderer model;
    private final TerrainRegistry type;

    protected final List<Collider<?>> preciseCollider = new ArrayList<>();

    private Material overrideMaterial = null;

    protected Mask collisionMask = new Mask();

    public Terrain(Resource model, TerrainRegistry type) {
        this.type = type;
        this.model = ModelManager.load(model);
        this.updateAABB();
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        super.render(camera, matrices, delta);

        matrices.pushMatrix();
        matrices.translate(0.5f, 0f, 0.5f);
        transform.applyTransform(matrices);

        renderModel(camera, overrideMaterial, matrices, delta);

        matrices.popMatrix();
    }

    protected void renderModel(Camera camera, Material material, MatrixStack matrices, float delta) {
        if (model != null)
            model.render(matrices, material);
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        DebugRenderer.renderAABB(matrices, aabb, 0xFFFFFFFF);

        for (Collider<?> collider : preciseCollider)
            DebugRenderer.renderShape(matrices, collider, 0xFFFF00FF);
    }

    protected void updateAABB() {
        Matrix4f mat = new Matrix4f().translate(0.5f, 0f, 0.5f).mul(transform.getMatrix().pos());

        if (model == null) {
            aabb.set(0, 0, 0, 1, 1, 1).applyMatrix(mat);
            preciseCollider.clear();
            preciseCollider.add(aabb);
            return;
        }

        this.aabb.set(this.model.getAABB()).applyMatrix(mat);

        this.preciseCollider.clear();
        for (AABB group : this.model.getPreciseAABB())
            preciseCollider.add(new OBB(group).applyMatrix(mat));

        updateTerrainInWorld();
    }

    protected void updateTerrainInWorld() {
        World w = getWorld();
        if (w != null)
            w.updateTerrain(this);
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return camera.getPos().distanceSquared(transform.getPos()) <= getRenderDistance() && super.shouldRender(camera);
    }

    public int getRenderDistance() {
        int dist = WorldRenderer.renderDistance;
        return dist * dist;
    }

    public Animation getAnimation(String name) {
        return model instanceof AnimatedObjRenderer anim ? anim.getAnimation(name) : null;
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.transform.setPos(x, y, z);
        this.updateAABB();
    }

    public World getWorld() {
        return world;
    }

    public List<Collider<?>> getPreciseCollider() {
        return preciseCollider;
    }

    public void setRotation(Quaternionf rotation) {
        this.transform.setRot(rotation);
        this.updateAABB();
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.transform.setRot(pitch, yaw, roll);
        this.updateAABB();
    }

    public void setScale(float scalar) {
        this.setScale(scalar, scalar, scalar);
    }

    public void setScale(Vector3f scale) {
        this.setScale(scale.x, scale.y, scale.z);
    }

    public void setScale(float x, float y, float z) {
        this.transform.setScale(x, y, z);
        this.updateAABB();
    }

    public void setMaterial(Material material) {
        this.overrideMaterial = material;
    }

    public Material getMaterial() {
        return overrideMaterial;
    }

    @Override
    public TerrainRegistry getType() {
        return type;
    }

    public boolean isSelectable(Entity entity) {
        return true;
    }

    public Mask getCollisionMask() {
        return collisionMask;
    }
}
