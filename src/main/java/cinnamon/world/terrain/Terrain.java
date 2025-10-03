package cinnamon.world.terrain;

import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.model.material.Material;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.world.WorldObject;
import cinnamon.world.entity.Entity;
import cinnamon.world.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Terrain extends WorldObject {

    protected final ModelRenderer model;
    private final TerrainRegistry type;

    protected final List<AABB> preciseAABB = new ArrayList<>(); //group's AABB

    private byte rotation = 0;
    private MaterialRegistry overrideMaterial = MaterialRegistry.DEFAULT;

    public Terrain(Resource model, TerrainRegistry type) {
        this.type = type;
        this.model = ModelManager.load(model);
        this.updateAABB();
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        super.render(camera, matrices, delta);

        matrices.pushMatrix();

        matrices.translate(pos.x + 0.5f, pos.y, pos.z + 0.5f);
        matrices.rotate(Rotation.Y.rotationDeg(getRotationAngle()));

        renderModel(camera, overrideMaterial.material, matrices, delta);

        matrices.popMatrix();
    }

    protected void renderModel(Camera camera, Material material, MatrixStack matrices, float delta) {
        if (model != null)
            model.render(matrices, material);
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        renderAABB(matrices, aabb, 0xFFFFFFFF);

        for (AABB aabb : preciseAABB)
            renderAABB(matrices, aabb, 0xFFFF00FF);
    }

    private static void renderAABB(MatrixStack matrices, AABB aabb, int color) {
        Vector3f min = aabb.getMin(); Vector3f max = aabb.getMax();
        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, min.x, min.y, min.z, max.x, max.y, max.z, color));
    }

    protected void updateAABB() {
        if (model == null) {
            aabb.set(pos).expand(1f, 1f, 1f);
            preciseAABB.clear();
            preciseAABB.add(aabb);
            return;
        }

        float r = getRotationAngle();
        this.aabb.set(this.model.getAABB()).rotateY(r).translate(pos.x + 0.5f, pos.y, pos.z + 0.5f);

        this.preciseAABB.clear();
        for (AABB group : this.model.getPreciseAABB())
            preciseAABB.add(group.rotateY(r).translate(pos.x + 0.5f, pos.y, pos.z + 0.5f));

        World w = getWorld();
        if (w != null) {
            w.removeTerrain(this);
            w.addTerrain(this);
        }
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return camera.getPos().distanceSquared(getPos()) <= getRenderDistance() && super.shouldRender(camera);
    }

    public int getRenderDistance() {
        int dist = WorldRenderer.renderDistance;
        return dist * dist;
    }

    @Override
    public void setPos(float x, float y, float z) {
        super.setPos(x, y, z);
        this.updateAABB();
    }

    public World getWorld() {
        return world;
    }

    public List<AABB> getPreciseAABB() {
        return preciseAABB;
    }

    public void setRotation(byte rotation) {
        this.rotation = rotation;
        this.updateAABB();
    }

    public int getRotation() {
        return rotation;
    }

    public float getRotationAngle() {
        return 90f * rotation;
    }

    public void setMaterial(MaterialRegistry material) {
        this.overrideMaterial = material;
    }

    public MaterialRegistry getMaterial() {
        return overrideMaterial;
    }

    @Override
    public TerrainRegistry getType() {
        return type;
    }

    public boolean isSelectable(Entity entity) {
        return true;
    }
}
