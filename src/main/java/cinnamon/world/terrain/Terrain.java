package cinnamon.world.terrain;

import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.Model;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.AABB;
import cinnamon.utils.Rotation;
import cinnamon.world.WorldObject;
import cinnamon.world.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Terrain extends WorldObject {

    protected final Model model;
    private final TerrainRegistry type;

    private AABB aabb; //the entire model's AABB
    private final List<AABB> groupsAABB = new ArrayList<>(); //group's AABB

    private byte rotation = 0;
    private MaterialRegistry overrideMaterial;

    public Terrain(TerrainRegistry type) {
        this.type = type;
        this.model = getType().model;
        this.updateAABB();
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        matrices.translate(pos.x + 0.5f, pos.y, pos.z + 0.5f);
        matrices.rotate(Rotation.Y.rotationDeg(getRotationAngle()));

        renderModel(matrices, delta);

        matrices.pop();
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        //render model
        model.render(matrices, overrideMaterial.material);
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        renderAABB(matrices, aabb, 0xFFFFFFFF);

        for (AABB aabb : groupsAABB)
            renderAABB(matrices, aabb, 0xFFFF00FF);
    }

    private static void renderAABB(MatrixStack matrices, AABB aabb, int color) {
        Vector3f min = aabb.getMin(); Vector3f max = aabb.getMax();
        VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, min.x, min.y, min.z, max.x, max.y, max.z, color));
    }

    protected void updateAABB() {
        float r = (float) Math.toRadians(getRotationAngle());
        this.aabb = this.model.getMeshAABB().rotateY(r).translate(pos.x + 0.5f, pos.y, pos.z + 0.5f);

        this.groupsAABB.clear();
        for (AABB group : this.model.getGroupsAABB())
            groupsAABB.add(group.rotateY(r).translate(pos.x + 0.5f, pos.y, pos.z + 0.5f));
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

    public List<AABB> getGroupsAABB() {
        return groupsAABB;
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
}
