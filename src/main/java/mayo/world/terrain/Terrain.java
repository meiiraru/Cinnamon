package mayo.world.terrain;

import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.utils.AABB;
import mayo.utils.Rotation;
import mayo.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public abstract class Terrain {

    protected final Model model;
    private final World world;
    private final Vector3f pos = new Vector3f();

    private AABB aabb; //the entire model's AABB
    private final List<AABB> groupsAABB = new ArrayList<>(); //group's AABB

    private int rotation = 0;

    public Terrain(Model model, World world) {
        this.model = model;
        this.world = world;
        this.updateAABB();
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        matrices.translate(pos);
        matrices.rotate(Rotation.Y.rotationDeg(getRotationAngle()));

        renderModel(matrices, delta);

        matrices.pop();
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        //render model
        Shader.activeShader.setMatrixStack(matrices);
        model.render();
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        renderAABB(matrices, aabb, -1);

        for (AABB aabb : groupsAABB)
            renderAABB(matrices, aabb, 0xFFFF00FF);
    }

    private static void renderAABB(MatrixStack matrices, AABB aabb, int color) {
        Vector3f min = aabb.getMin(); Vector3f max = aabb.getMax();
        GeometryHelper.pushCube(VertexConsumer.LINES, matrices, min.x, min.y, min.z, max.x, max.y, max.z, color);
    }

    protected void updateAABB() {
        float r = (float) Math.toRadians(getRotationAngle());
        this.aabb = this.model.getMeshAABB().rotateY(r).translate(pos);

        this.groupsAABB.clear();
        for (AABB group : this.model.getGroupsAABB())
            groupsAABB.add(group.rotateY(r).translate(pos));
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.pos.set(x, y, z);
        this.updateAABB();
    }

    public Vector3f getPos() {
        return pos;
    }

    public World getWorld() {
        return world;
    }

    public AABB getAABB() {
        return aabb;
    }

    public List<AABB> getGroupsAABB() {
        return groupsAABB;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public float getRotationAngle() {
        return 90f * rotation;
    }
}
