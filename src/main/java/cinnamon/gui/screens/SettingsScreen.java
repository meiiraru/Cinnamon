package cinnamon.gui.screens;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.ContainerTabs;
import cinnamon.gui.widgets.types.Button;
import cinnamon.render.MatrixStack;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;

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
    public void init() {
        ContainerTabs tabs = new ContainerTabs(width / 2, 20, width - 100, 20);
        tabs.setAlignment(Alignment.TOP_CENTER);
        addWidget(tabs);

        for (int i = 1; i <= 4; i++) {
            ContainerGrid container = new ContainerGrid(0, 0, 1);
            for (int j = 1; j <= 5; j++)
                container.addWidget(new Button(0, 0, i * 100, 20, Text.of("Setting " + i + "x" + j), b -> {}));
            tabs.addTab(Text.of("Tab " + i), container);
        }
        tabs.setPage(0);

        super.init();
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        if (fromWorld)
            renderSolidBackground(0x88 << 24);
        else
            super.renderBackground(matrices, delta, color1, color2, size);
    }
}