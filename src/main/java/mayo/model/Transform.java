package mayo.model;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Transform {

    private final Vector3f
            pos = new Vector3f(),
            rot = new Vector3f(),
            pivot = new Vector3f(),
            scale = new Vector3f(1f, 1f, 1f),
            color = new Vector3f(1f, 1f, 1f);
    private final Vector2f
            uv = new Vector2f();

    private final Matrix4f
            positionMatrix = new Matrix4f();
    private final Matrix3f
            normalMatrix = new Matrix3f();

    private boolean
            dirty = true,
            matrixDirty = true;

    public void dirty() {
        this.dirty = true;
        this.matrixDirty = true;
    }

    public void clean() {
        this.dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

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

    public Vector3f getColor() {
        return color;
    }

    public Vector2f getUV() {
        return uv;
    }

    public void setPos(Vector3f pos) {
        this.pos.set(pos);
        dirty();
    }

    public void setRot(Vector3f rot) {
        this.rot.set(rot);
        dirty();
    }

    public void setPivot(Vector3f pivot) {
        this.pivot.set(pivot);
        dirty();
    }

    public void setScale(Vector3f scale) {
        this.scale.set(scale);
        dirty();
    }

    public void setColor(Vector3f color) {
        this.color.set(color);
        dirty();
    }

    public void setUV(Vector2f uv) {
        this.uv.set(uv);
        dirty();
    }

    public void setPositionMatrix(Matrix4f positionMatrix) {
        this.positionMatrix.set(positionMatrix);
        this.matrixDirty = false;
    }

    public void setNormalMatrix(Matrix3f normalMatrix) {
        this.positionMatrix.set(normalMatrix);
    }
}
