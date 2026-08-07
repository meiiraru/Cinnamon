package cinnamon.gui.screens;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.Container;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.ContainerTabs;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.render.MatrixStack;
import cinnamon.settings.Setting;
import cinnamon.settings.Settings;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        createCategories(tabs);
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

    protected void createCategories(ContainerTabs tabs) {
        //major categories
        Category root = new Category();

        for (Setting<?> setting : Settings.SETTINGS) {
            String name = setting.getName();
            String[] split = name.split("\\.");

            Category curr = root;
            int i = 0;
            while (i < split.length) {
                //last part of the split, add the raw setting to the list
                if (i == split.length - 1) {
                    curr.settings.add(setting);
                    break;
                }

                //otherwise, create a new subcategory if it does not exist and move to it
                String category = split[i];
                curr = curr.subcategories.computeIfAbsent(category, k -> new Category());
                i++;
            }
        }

        //create and populate tabs for each category
        for (Map.Entry<String, Category> entry : root.subcategories.entrySet()) {
            WidgetList list = new WidgetList(0, 0, width - 8, height - tabs.getTabsYOffset() - 4 - 4 - 20 - 12, 12);
            populateList(list, entry.getValue(), "setting." + entry.getKey());
            tabs.addTab(Text.translated("settings.category." + entry.getKey()), list);
        }
    }

    protected void populateList(Container container, Category settings, String parentKey) {
        //add settings to the list
        if (!settings.settings.isEmpty()) {
            ContainerGrid grid = new ContainerGrid(0, 0, 4);
            container.addWidget(grid);

            for (Setting<?> setting : settings.settings)
                createSetting(grid, setting);
        }

        //add subcategories to the list
        for (Map.Entry<String, Category> entry : settings.subcategories.entrySet()) {
            String newKey = parentKey + "." + entry.getKey();
            container.addWidget(new Label(0, 0, Text.translated(newKey), Alignment.CENTER));
            populateList(container, entry.getValue(), newKey);
        }
    }

    protected void createSetting(Container container, Setting<?> setting) {
        container.addWidget(new Button(0, 0, 200, 20, Text.translated("setting." + setting.getName()), b -> {}));
    }

    protected static class Category {
        public final List<Setting<?>> settings = new ArrayList<>();
        public final Map<String, Category> subcategories = new LinkedHashMap<>();
    }
}