package cinnamon.world.gui;

import cinnamon.render.MatrixStack;

public abstract class Overlay {

    protected boolean closed = true;
    protected boolean stealMouse = false;

    public void open() {
        this.closed = false;
    }

    public void close() {
        this.closed = true;
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {}

    public boolean stealsMouse() {
        return stealMouse;
    }

    public boolean mousePress(int button, int action, int mods) {
        return false;
    }

    public boolean isClosed() {
        return closed;
    }
}
