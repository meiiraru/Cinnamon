package cinnamon.gui.screens;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.render.MatrixStack;

public class SettingsScreen extends ParentedScreen {

    private final boolean fromWorld;

    public SettingsScreen(Screen parentScreen) {
        this(parentScreen, false);
    }

    public SettingsScreen(Screen parentScreen, boolean fromWorld) {
        super(parentScreen);
        this.fromWorld = fromWorld;
    }


    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        if (fromWorld)
            renderSolidBackground(0x88 << 24);
        else
            super.renderBackground(matrices, delta, color1, color2, size);
    }
}