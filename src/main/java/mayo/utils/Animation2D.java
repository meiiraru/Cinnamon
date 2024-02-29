package mayo.utils;

import mayo.render.MatrixStack;

public abstract class Animation2D {

    protected float x, y;
    protected float changeX, changeY;
    protected float anchorX, anchorY;

    protected Animation2D(float x, float y) {
        this.changeX = x;
        this.changeY = y;
    }

    public void tick() {
        x += changeX;
        y += changeY;
    }

    public void setChange(float x, float y) {
        changeX = x;
        changeY = y;
    }

    public void setAnchor(float x, float y) {
        anchorX = x;
        anchorY = y;
    }

    public float getX(float delta) {
        return x + changeX * delta;
    }

    public float getY(float delta) {
        return y + changeY * delta;
    }

    public abstract void apply(MatrixStack matrices, float delta);

    public static class Translate extends Animation2D {
        public Translate(float moveX, float moveY) {
            super(moveX, moveY);
        }

        @Override
        public void apply(MatrixStack matrices, float delta) {
            matrices.translate(getX(delta), getY(delta), 0f);
        }
    }

    public static class Rotate extends Animation2D {
        public Rotate(float rot) {
            super(rot, 0f);
        }

        @Override
        public void apply(MatrixStack matrices, float delta) {
            boolean anchor = anchorX != 0f || anchorY != 0f;
            if (anchor) matrices.translate(anchorX, anchorY, 0f);
            matrices.rotate(Rotation.Z.rotationDeg(getX(delta)));
            if (anchor) matrices.translate(-anchorX, -anchorY, 0f);
        }
    }

    public static class Scale extends Animation2D {
        public Scale(float scaleX, float scaleY) {
            super(scaleX, scaleY);
            x = y = 1f;
        }

        @Override
        public void apply(MatrixStack matrices, float delta) {
            boolean anchor = anchorX != 0f || anchorY != 0f;
            if (anchor) matrices.translate(anchorX, anchorY, 0f);
            matrices.scale(getX(delta), getY(delta), 0f);
            if (anchor) matrices.translate(-anchorX, -anchorY, 0f);
        }
    }
}
