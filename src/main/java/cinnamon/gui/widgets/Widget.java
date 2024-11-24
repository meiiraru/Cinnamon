package cinnamon.gui.widgets;

import cinnamon.render.MatrixStack;

public abstract class Widget {

    private int x, y;
    private int width, height;
    private Widget parent;

    public Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(MatrixStack matrices, int mouseX, int mouseY, float delta);

    public void setPos(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

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

    public void setDimensions(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public int getCenterX() {
        return getX() + getWidth() / 2;
    }

    public int getCenterY() {
        return getY() + getHeight() / 2;
    }

    public void translate(int x, int y) {
        setPos(getX() + x, getY() + y);
    }

    public void setParent(Widget parent) {
        this.parent = parent;
    }

    public Widget getParent() {
        return parent;
    }
}
