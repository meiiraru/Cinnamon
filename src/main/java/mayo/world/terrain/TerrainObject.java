package mayo.world.terrain;

import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.utils.AABB;
import mayo.world.World;
import org.joml.Vector3f;

public abstract class TerrainObject {

    protected final Model model;
    protected final World world;
    protected final Vector3f dimensions = new Vector3f();
    protected final Vector3f pos = new Vector3f();

    protected AABB aabb;

    protected TerrainObject(Model model, World world) {
        this.model = model;
        this.world = world;
        this.dimensions.set(model.getMesh().getBoundingBox());
        this.updateAABB();
    }

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        matrices.translate(pos);

        renderModel(matrices, delta);

        matrices.pop();

        //renderDebugHitbox(matrices, delta);
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        //render model
        Shader.activeShader.setMatrixStack(matrices);
        model.render();
    }

    protected void renderDebugHitbox(MatrixStack matrices, float delta) {
        if (world.isDebugRendering()) {
            Vector3f min = aabb.getMin();
            Vector3f max = aabb.getMax();
            GeometryHelper.pushCube(VertexConsumer.MAIN, matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0x88FFFFFF);
        }
    }

    protected void updateAABB() {
        Vector3f min = this.model.getMesh().getBBMin();
        Vector3f max = this.model.getMesh().getBBMax();
        this.aabb = new AABB(
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
}
