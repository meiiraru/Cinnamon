package mayo.gui.widgets;

import mayo.render.MatrixStack;

public abstract class Widget {

    private int x, y;
    private int width, height;

    public Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(MatrixStack matrices, float delta);

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }

    protected void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    protected void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }
}
