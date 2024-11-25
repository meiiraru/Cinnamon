package cinnamon.model;

import cinnamon.render.MatrixStack;
import cinnamon.utils.Rotation;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Transform {

    private final Vector4f
            color = new Vector4f(1f, 1f, 1f, 1f);
    private final Vector3f
            pos = new Vector3f(),
            rot = new Vector3f(),
            pivot = new Vector3f(),
            scale = new Vector3f(1f, 1f, 1f);
    private final Vector2f
            uv = new Vector2f();

    public void applyTransform(MatrixStack matrices) {
        matrices.translate(pivot);

        matrices.scale(scale.x, scale.y, scale.z);

        matrices.rotate(Rotation.Z.rotationDeg(rot.z));
        matrices.rotate(Rotation.Y.rotationDeg(rot.y));
        matrices.rotate(Rotation.X.rotationDeg(rot.x));

        matrices.translate(
                pos.x - pivot.x,
                pos.y - pivot.y,
                pos.z - pivot.z
        );

        scale.set(1f);
        rot.set(0f);
        pos.set(0f);
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getRot() {
        return rot;
    }

    public Vector3f getPivot() {
        return pivot;
    }

    public Vector3f getScale() {
        return scale;
    }

    public Vector4f getColor() {
        return color;
    }

    public Vector2f getUV() {
        return uv;
    }

    public void setPos(Vector3f vec) {
        this.setPos(vec.x, vec.y, vec.z);
    }

    public void setPos(float x, float y, float z) {
        this.pos.set(x, y, z);
    }

    public void setRot(Vector3f vec) {
        this.setRot(vec.x, vec.y, vec.z);
    }

    public void setRot(float x, float y, float z) {
        this.rot.set(x, y, z);
    }

    public void setPivot(Vector3f vec) {
        this.setPivot(vec.x, vec.y, vec.z);
    }

    public void setPivot(float x, float y, float z) {
        this.pivot.set(x, y, z);
    }

    public void setScale(float scalar) {
        this.setScale(scalar, scalar, scalar);
    }

    public void setScale(Vector3f vec) {
        this.setScale(vec.x, vec.y, vec.z);
    }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
    }

    public void setColor(Vector3f vec) {
        this.setColor(vec.x, vec.y, vec.z);
    }

    public void setColor(float r, float g, float b) {
        this.setColor(r, g, b, 1f);
    }

    public void setColor(Vector4f vec) {
        this.setColor(vec.x, vec.y, vec.z, vec.w);
    }

    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
    }

    public void setUV(Vector2f vec) {
        this.setUV(vec.x, vec.y);
    }

    public void setUV(float x, float y) {
        this.uv.set(x, y);
    }
}
