package mayo.world.entity;

import mayo.Client;
import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.utils.AABB;
import mayo.utils.Maths;
import mayo.utils.Rotation;
import mayo.world.DamageType;
import mayo.world.World;
import mayo.world.entity.living.LivingEntity;
import org.joml.Vector2f;
import org.joml.Vector3f;

public abstract class Entity {

    protected final Model model;
    protected final World world;
    protected final Vector3f
            oPos = new Vector3f(),
            pos = new Vector3f();
    protected final Vector2f
            oRot = new Vector2f(),
            rot = new Vector2f();

    protected AABB aabb;

    private boolean removed;

    public Entity(Model model, World world) {
        this.model = model;
        this.world = world;
        this.updateAABB();
    }

    public void tick() {
        this.oPos.set(pos);
        this.oRot.set(rot);
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
        renderDebugHitbox(matrices, delta);
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        matrices.push();

        //apply rot
        applyModelPose(matrices, delta);

        //render model
        Shader.activeShader.setMatrixStack(matrices);
        model.render();

        //render features
        renderFeatures(matrices, delta);

        matrices.pop();
    }

    protected void applyModelPose(MatrixStack matrices, float delta) {
        Vector2f rot = getRot(delta);
        matrices.rotate(Rotation.Y.rotationDeg(-rot.y));
        matrices.rotate(Rotation.X.rotationDeg(-rot.x));
    }

    protected void renderFeatures(MatrixStack matrices, float delta) {}

    protected void renderTexts(MatrixStack matrices, float delta) {}

    public boolean shouldRenderText() {
        Vector3f cam = Client.getInstance().camera.getPos();
        return cam.distanceSquared(pos) <= 256;
    }

    protected void renderDebugHitbox(MatrixStack matrices, float delta) {
        if (!world.isDebugRendering())
            return;

        Vector3f cam = Client.getInstance().camera.getPos();
        if (cam.distanceSquared(pos) > 256)
            return;

        //bounding box
        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();
        GeometryHelper.pushCube(VertexConsumer.LINES, matrices, min.x, min.y, min.z, max.x, max.y, max.z, -1);

        //eye pos
        Vector3f eye = getEyePos();
        if (this instanceof LivingEntity) {
            float f = 0.01f;
            GeometryHelper.pushCube(VertexConsumer.LINES, matrices, min.x, eye.y - f, min.z, max.x, eye.y + f, max.z, 0xFFFF0000);
        }

        //looking dir
        matrices.push();
        matrices.translate(eye);
        matrices.rotate(Rotation.Y.rotationDeg(-rot.y + 90));
        matrices.rotate(Rotation.Z.rotationDeg(-rot.x));

        VertexConsumer.LINES.consume(GeometryHelper.quad(
                matrices,
                0f, 0f, 0f,
                aabb.getWidth() + 1f, 0f,
                0xFF0000FF

        ), 0);

        matrices.pop();
    }

    public void onAdd() {}

    public void remove() {
        this.removed = true;
    }

    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        return false;
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

    public void moveTo(Vector3f vec) {
        this.moveTo(vec.x, vec.y, vec.z);
    }

    public void moveTo(float x, float y, float z) {
        this.pos.set(x, y, z);
        this.updateAABB();
    }

    public void rotate(Vector2f rot) {
        this.rotate(rot.x, rot.y);
    }

    public void rotate(float pitch, float yaw) {
        this.rot.set(pitch, yaw);
    }

    public void lookAt(Vector3f pos) {
        this.lookAt(pos.x, pos.y, pos.z);
    }

    public void lookAt(float x, float y, float z) {
        //get difference
        Vector3f v = new Vector3f(x, y, z).sub(getEyePos());
        v.normalize();

        //set rot
        this.setRot(Maths.dirToRot(v));
    }

    protected void updateAABB() {
        this.aabb = this.model.getMesh().getAABB().translate(pos);
    }

    public Vector3f getPos(float delta) {
        return Maths.lerp(oPos, pos, delta);
    }

    public Vector3f getPos() {
        return pos;
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.oPos.set(this.pos.set(x, y, z));
        this.updateAABB();
    }

    public Vector2f getRot(float delta) {
        return Maths.lerp(oRot, rot, delta);
    }

    public Vector2f getRot() {
        return rot;
    }

    public void setRot(Vector2f rot) {
        this.setRot(rot.x, rot.y);
    }

    public void setRot(float pitch, float yaw) {
        this.oRot.set(this.rot.set(pitch, yaw));
    }

    public float getEyeHeight() {
        return aabb.getHeight() * 0.5f;
    }

    public Vector3f getEyePos(float delta) {
        return getPos(delta).add(0, getEyeHeight(), 0);
    }

    public Vector3f getEyePos() {
        return new Vector3f(pos.x, pos.y + getEyeHeight(), pos.z);
    }

    public Vector3f getLookDir() {
        return Maths.rotToDir(rot.x, rot.y);
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
