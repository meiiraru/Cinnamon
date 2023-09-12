package mayo.gui.widgets;

import mayo.render.MatrixStack;

public interface Widget {

    void render(MatrixStack stack);

    default boolean mousePress(int button, int action, int mods) {
        return false;
    }

    default boolean keyPress(int key, int scancode, int action, int mods) {
        return false;
    }

    default boolean charTyped(char c, int mods) {
        return false;
    }

    default boolean mouseMove(double x, double y) {
        return false;
    }

    default boolean scroll(double x, double y) {
        return false;
    }

    default boolean windowResize(int width, int height) {
        return false;
    }

    default boolean windowFocused(boolean focused) {
        return false;
    }
}
