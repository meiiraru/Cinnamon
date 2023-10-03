package mayo.world.terrain;

import mayo.Client;
import mayo.model.GeometryHelper;
import mayo.model.obj.Group;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.utils.AABB;
import mayo.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public abstract class Terrain {

    protected final Model model;
    private final World world;
    private final Vector3f dimensions = new Vector3f();
    private final Vector3f pos = new Vector3f();

    private AABB aabb; //the entire model's AABB
    private final List<AABB> groupsAABB = new ArrayList<>(); //group's AABB

    public Terrain(Model model, World world) {
        this.model = model;
        this.world = world;
        this.dimensions.set(model.getMesh().getBoundingBox());
        this.updateAABB();
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        matrices.translate(pos);

        renderModel(matrices, delta);

        matrices.pop();

        renderDebugHitbox(matrices, delta);
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        //render model
        Shader.activeShader.setMatrixStack(matrices);
        model.render();
    }

    protected void renderDebugHitbox(MatrixStack matrices, float delta) {
        if (!world.isDebugRendering())
            return;

        Vector3f cam = Client.getInstance().camera.getPos();
        if (cam.distanceSquared(pos) > 256)
            return;

        renderAABB(matrices, aabb, -1);

        for (AABB aabb : groupsAABB)
            renderAABB(matrices, aabb, 0xFFFF00FF);
    }

    private static void renderAABB(MatrixStack matrices, AABB aabb, int color) {
        Vector3f min = aabb.getMin(); Vector3f max = aabb.getMax();
        GeometryHelper.pushCube(VertexConsumer.LINES, matrices, min.x, min.y, min.z, max.x, max.y, max.z, color);
    }

    protected void updateAABB() {
        this.aabb = getAABB(this.model.getMesh().getBBMin(), this.model.getMesh().getBBMax());

        this.groupsAABB.clear();
        for (Group group : this.model.getMesh().getGroups())
            groupsAABB.add(getAABB(group.getBBMin(), group.getBBMax()));
    }

    private AABB getAABB(Vector3f min, Vector3f max) {
        return new AABB(
                pos.x + min.x, pos.y + min.y, pos.z + min.z,
                pos.x + max.x, pos.y + max.y, pos.z + max.z
        );
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

    public Vector3f getDimensions() {
        return dimensions;
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
}
