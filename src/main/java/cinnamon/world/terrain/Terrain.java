package cinnamon.world.terrain;

import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.MatrixStack;
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

    protected AABB aabb; //the entire model's AABB
    protected final List<AABB> preciseAABB = new ArrayList<>(); //group's AABB

    private byte rotation = 0;
    private MaterialRegistry overrideMaterial = MaterialRegistry.DEFAULT;

    public Terrain(Resource model, TerrainRegistry type) {
        this.type = type;
        this.model = ModelManager.load(model);
        this.updateAABB();
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        matrices.pushMatrix();

        matrices.translate(pos.x + 0.5f, pos.y, pos.z + 0.5f);
        matrices.rotate(Rotation.Y.rotationDeg(getRotationAngle()));

        renderModel(matrices, delta);

        matrices.popMatrix();
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        //render model
        if (model != null)
            model.render(matrices, overrideMaterial.material);
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        renderAABB(matrices, aabb, 0xFFFFFFFF);

        for (AABB aabb : preciseAABB)
            renderAABB(matrices, aabb, 0xFFFF00FF);
    }

    private static void renderAABB(MatrixStack matrices, AABB aabb, int color) {
        Vector3f min = aabb.getMin(); Vector3f max = aabb.getMax();
        VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, min.x, min.y, min.z, max.x, max.y, max.z, color));
    }

    protected void updateAABB() {
        if (model == null) {
            aabb = new AABB(pos, pos).expand(1f, 1f, 1f);
            preciseAABB.clear();
            preciseAABB.add(aabb);
            return;
        }

        float r = getRotationAngle();
        this.aabb = this.model.getAABB().rotateY(r).translate(pos.x + 0.5f, pos.y, pos.z + 0.5f);

        this.preciseAABB.clear();
        for (AABB group : this.model.getPreciseAABB())
            preciseAABB.add(group.rotateY(r).translate(pos.x + 0.5f, pos.y, pos.z + 0.5f));
    }

    @Override
    public void setPos(float x, float y, float z) {
        super.setPos(x, y, z);
        this.updateAABB();
    }

    public World getWorld() {
        return world;
    }

    @Override
    public AABB getAABB() {
        return aabb;
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
