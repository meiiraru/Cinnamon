package mayo.model;

import org.joml.*;

import java.lang.Math;

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

    private final Matrix4f
            positionMatrix = new Matrix4f();
    private final Matrix3f
            normalMatrix = new Matrix3f();

    private boolean matrixDirty = true;

    private void recalculateMatrices() {
        if (!matrixDirty)
            return;

        //position
        positionMatrix.identity();

        //position the pivot point at 0, 0, 0, and translate the model
        positionMatrix.translate(-pivot.x, -pivot.y, -pivot.z);

        //scale the model part around the pivot
        positionMatrix.scale(scale);

        //rotate the model part around the pivot
        positionMatrix.rotateZYX(rot);

        //undo the effects of the pivot translation
        positionMatrix.translate(
                pos.x + pivot.x,
                pos.y + pivot.y,
                pos.z + pivot.z
        );

        //normal
        normalMatrix.identity();

        float x = scale.x;
        float y = scale.y;
        float z = scale.z;
        float c = (float) Math.cbrt(x * y * z);
        normalMatrix.scale(
                c == 0 && x == 0 ? 1 : c / x,
                c == 0 && y == 0 ? 1 : c / y,
                c == 0 && z == 0 ? 1 : c / z
        );

        normalMatrix.rotateZYX(rot);

        matrixDirty = false;
    }

    public Matrix4f getPositionMatrix() {
        recalculateMatrices();
        return positionMatrix;
    }

    public Matrix3f getNormalMatrix() {
        recalculateMatrices();
        return normalMatrix;
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
        matrixDirty = true;
    }

    public void setRot(Vector3f vec) {
        this.setRot(vec.x, vec.y, vec.z);
    }

    public void setRot(float x, float y, float z) {
        this.rot.set(x, y, z);
        matrixDirty = true;
    }

    public void setPivot(Vector3f vec) {
        this.setPivot(vec.x, vec.y, vec.z);
    }

    public void setPivot(float x, float y, float z) {
        this.pivot.set(x, y, z);
        matrixDirty = true;
    }

    public void setScale(Vector3f vec) {
        this.setScale(vec.x, vec.y, vec.z);
    }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        matrixDirty = true;
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

    public void setPositionMatrix(Matrix4f positionMatrix) {
        this.positionMatrix.set(positionMatrix);
        this.matrixDirty = false;
    }

    public void setNormalMatrix(Matrix3f normalMatrix) {
        this.positionMatrix.set(normalMatrix);
    }
}
