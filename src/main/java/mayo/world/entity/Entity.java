package mayo.world.entity;

import mayo.Client;
import mayo.model.GeometryHelper;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.utils.AABB;
import mayo.utils.Meth;
import mayo.utils.Rotation;
import mayo.world.World;
import org.joml.Vector2f;
import org.joml.Vector3f;

public abstract class Entity {

    protected final Mesh model;
    protected final World world;
    protected final Vector3f dimensions = new Vector3f();
    protected final Vector3f
            oPos = new Vector3f(),
            pos = new Vector3f();
    protected final Vector2f
            oRot = new Vector2f(),
            rot = new Vector2f();

    protected AABB aabb;

    protected boolean removed;

    public Entity(Mesh model, World world, Vector3f dimensions) {
        this.model = model;
        this.world = world;
        this.dimensions.set(dimensions);
        this.updateAABB();
    }

    public void tick() {
        for (Entity entity : world.getEntities(getAABB()))
            if (entity != this && !entity.isRemoved())
                collide(entity);
    }

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        //apply pos
        matrices.translate(getPos(delta));

        //render model
        renderModel(matrices, delta);

        //render head text
        if (shouldRenderText())
            renderTexts(matrices, delta);

        matrices.pop();

        //render debug hitbox
        if (world.isDebugRendering()) {
            Vector3f min = aabb.getMin();
            Vector3f max = aabb.getMax();
            GeometryHelper.pushCube(VertexConsumer.MAIN, matrices.peek(), min.x, min.y, min.z, max.x, max.y, max.z, 0x88FFFFFF);
        }
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        matrices.push();

        //apply rot
        matrices.rotate(Rotation.Y.rotationDeg(-Meth.lerp(oRot.y, rot.y, delta)));
        matrices.rotate(Rotation.X.rotationDeg(-Meth.lerp(oRot.x, rot.x, delta)));

        //render model
        Shader.activeShader.setModelMatrix(matrices.peek());
        model.render();

        matrices.pop();
    }

    protected void renderTexts(MatrixStack matrices, float delta) {}

    public boolean shouldRenderText() {
        Vector3f cam = Client.getInstance().camera.getPos();
        return cam.distanceSquared(pos) < 256;
    }

    public void move(float left, float up, float forwards) {
        Vector3f move = new Vector3f(-left, up, -forwards);

        move.rotateX((float) Math.toRadians(-rot.x));
        move.rotateY((float) Math.toRadians(-rot.y));

        this.moveTo(
                pos.x + move.x,
                pos.y + move.y,
                pos.z + move.z
        );
    }

    public void moveTo(float x, float y, float z) {
        this.oPos.set(pos);
        this.pos.set(x, y, z);
        this.updateAABB();
    }

    public void rotate(float pitch, float yaw) {
        this.oRot.set(rot);
        this.rot.set(pitch, yaw);
    }

    public void lookAt(float x, float y, float z) {
        //get difference
        Vector3f v = new Vector3f(x, y, z).sub(getEyePos(1f));
        v.normalize();

        //set rot
        this.setRot(Meth.dirToRot(v));
    }

    protected void updateAABB() {
        Vector3f dim = new Vector3f(dimensions).mul(0.5f, 1f, 0.5f);
        float widthAndDepth = Math.max(dim.x, dim.z);
        this.aabb = new AABB(
                pos.x - widthAndDepth, pos.y, pos.z - widthAndDepth,
                pos.x + widthAndDepth, pos.y + dim.y, pos.z + widthAndDepth
        );
    }

    protected void collide(Entity entity) {}

    public Vector3f getPos(float delta) {
        return Meth.lerp(oPos, pos, delta);
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.oPos.set(this.pos.set(x, y, z));
        this.updateAABB();
    }

    public Vector2f getRot(float delta) {
        return Meth.lerp(oRot, rot, delta);
    }

    public void setRot(Vector2f rot) {
        this.setRot(rot.x, rot.y);
    }

    public void setRot(float pitch, float yaw) {
        this.oRot.set(this.rot.set(pitch, yaw));
    }

    public float getEyeHeight() {
        return 0f;
    }

    public Vector3f getEyePos(float delta) {
        return getPos(delta).add(0f, getEyeHeight(), 0f, new Vector3f());
    }

    public Vector3f getDimensions() {
        return dimensions;
    }

    public Vector3f getLookDir() {
        return Meth.rotToDir(rot.x, rot.y);
    }

    public AABB getAABB() {
        return aabb;
    }

    public World getWorld() {
        return world;
    }

    public boolean isRemoved() {
        return removed;
    }
}
