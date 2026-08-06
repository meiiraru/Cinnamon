package cinnamon.gui.screens;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.ContainerTabs;
import cinnamon.gui.widgets.WidgetList;
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
        ContainerTabs tabs = new ContainerTabs(width / 2, 4, width, 12);
        tabs.setAlignment(Alignment.TOP_CENTER);
        addWidget(tabs);

        for (int i = 1; i <= 10; i++) {
            WidgetList list = new WidgetList(0, 0, width - 8, height - tabs.getTabsYOffset() - 4 - 4 - 20 - 12, 1);
            for (int j = 1; j <= 50; j++)
                list.addWidget(new Button(0, 0, i * j, 20, Text.of(i + "x" + j), b -> {}));
            tabs.addTab(Text.of("Tab " + i), list);
        }
        tabs.setPage(0);

        //apply and cancel buttons
        ContainerGrid buttonGrid = new ContainerGrid(width / 2, height - 4, 12, 2);
        buttonGrid.setAlignment(Alignment.BOTTOM_CENTER);
        buttonGrid.addWidget(new Button(0, 0, 100, 20, Text.translated("gui.save"), b -> {}));
        buttonGrid.addWidget(new Button(0, 0, 100, 20, Text.translated("gui.cancel"), b -> close()));
        addWidget(buttonGrid);

        super.init();
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        if (fromWorld)
            renderSolidBackground(0x88 << 24);
        else
            super.renderBackground(matrices, delta, color1, color2, size);
    }
}