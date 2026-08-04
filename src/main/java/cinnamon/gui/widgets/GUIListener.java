package cinnamon.gui.widgets;

public interface GUIListener {

    default GUIListener windowFocused(boolean focused) {
        return null;
    }

    default GUIListener filesDropped(String[] files) {
        return null;
    }

    default GUIListener keyPress(int key, int scancode, int action, int mods) {
        return null;
    }

    default GUIListener charTyped(char c, int mods) {
        return null;
    }

    default GUIListener mousePress(int button, int action, int mods) {
        return null;
    }

    default GUIListener mouseMove(int x, int y) {
        return null;
    }

    default GUIListener mouseScroll(double x, double y) {
        return null;
    }
}
