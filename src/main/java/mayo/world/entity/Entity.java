package mayo.world.entity;

import mayo.Client;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.utils.Meth;
import mayo.utils.Rotation;
import mayo.world.World;
import org.joml.Vector2f;
import org.joml.Vector3f;

public abstract class Entity {

    private final Mesh model;
    private final World world;
    private final Vector3f dimensions;
    private final Vector3f
            oPos = new Vector3f(),
            pos = new Vector3f();
    private final Vector2f
            oRot = new Vector2f(),
            rot = new Vector2f();

    public Entity(Mesh model, World world, Vector3f dimensions) {
        this.model = model;
        this.world = world;
        this.dimensions = dimensions;
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        //apply pos
        matrices.translate(Meth.lerp(oPos, pos, delta));

        matrices.push();

        //apply rot
        matrices.rotate(Rotation.Y.rotationDeg(-Meth.lerp(oRot.y, rot.y, delta)));
        //matrices.rotate(Rotation.X.rotationDeg(-Meth.lerp(oRot.x, rot.x, delta)));

        //render model
        Shader.activeShader.setModelMatrix(matrices.peek());
        model.render();

        matrices.pop();

        //render head text
        if (shouldRenderText())
            renderTexts(matrices, delta);

        matrices.pop();
    }

    protected void renderTexts(MatrixStack matrices, float delta) {}

    public boolean shouldRenderText() {
        Vector3f cam = Client.getInstance().camera.getPos();
        return cam.distanceSquared(pos) < 256;
    }

    public void move(float left, float up, float forwards) {
        Vector3f vec = new Vector3f(left, up, -forwards);
        if (vec.lengthSquared() > 1f)
            vec.normalize();

        double rad = Math.toRadians(rot.y);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        this.moveTo(
                pos.x + (float) (vec.x * cos - vec.z * sin),
                pos.y + vec.y,
                pos.z + (float) (vec.z * cos + vec.x * sin)
        );
    }

    public void moveTo(float x, float y, float z) {
        this.oPos.set(pos);
        this.pos.set(x, y, z);
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

    public Vector3f getPos(float delta) {
        return Meth.lerp(oPos, pos, delta);
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.oPos.set(this.pos.set(x, y, z));
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
        return 1f;
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
}
