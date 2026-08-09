package cinnamon.gui.screens;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.Container;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.ContainerTabs;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.*;
import cinnamon.input.Keybind;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.settings.Setting;
import cinnamon.settings.Settings;
import cinnamon.text.Text;
import cinnamon.utils.*;
import org.joml.Math;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public class SettingsScreen extends ParentedScreen {

    public static final Resource RESET_ICON = new Resource("textures/gui/icons/reload.png");

    private final boolean fromWorld;

    private final List<Setting.Keybind> keybindList = new ArrayList<>();
    private Setting.Keybind currentKeybind;
    private Button keybindButton;

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

        keybindList.clear();
        createCategories(tabs);
        tabs.setPage(0);

        //control buttons
        ContainerGrid buttonGrid = new ContainerGrid(width / 2, height - 4, 12, 3);
        buttonGrid.setAlignment(Alignment.BOTTOM_CENTER);
        addWidget(buttonGrid);

        buttonGrid.addWidget(new Button(0, 0, 96, 20, Text.translated("gui.save"), b -> {
            saveSettings();
            discardSettings();
        }));

        buttonGrid.addWidget(new Button(0, 0, 96, 20, Text.translated("gui.discard"), b -> {
            discardSettings();
            rebuild();
        }));

        buttonGrid.addWidget(new Button(0, 0, 96, 20, Text.translated("gui.close"), b -> {
            boolean hasChanges = false;
            for (Setting<?> setting : Settings.SETTINGS) {
                if (setting.getTempValue() != null) {
                    hasChanges = true;
                    break;
                }
            }

            if (!hasChanges) {
                close();
                return;
            }

            ConfirmPopup.YesNo confirm = new ConfirmPopup.YesNo(Text.translated("gui.settings.save"), bool -> {
                if (bool) saveSettings();
                close();
            });
            UIHelper.setPopup(0, 0, confirm);
            confirm.open();
        }));

        super.init();
    }

    @Override
    public void close() {
        this.discardSettings();
        super.close();
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

    protected void saveSettings() {
        boolean hasChanges = false;

        //apply temporary values to actual values
        for (Setting<?> setting : Settings.SETTINGS) {
            hasChanges |= setting.getTempValue() != null;
            setting.applyTemp();
        }

        if (hasChanges)
            Settings.save();
    }

    protected void discardSettings() {
        //discard temporary values
        for (Setting<?> setting : Settings.SETTINGS)
            setting.discardTemp();
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
            grid.setAlignment(Alignment.TOP_CENTER);
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
        int border = 24;
        int resetW = 16;
        int spacing = 4;
        int widgetW = 160;
        int widgetH = 20;
        int x = width - border - border - resetW - spacing - widgetW;

        Container set = new Container(0, 0) {
            @Override
            protected void renderWidgets(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                if (UIHelper.isMouseOver(this, mouseX, mouseY))
                    VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, getX(), getY() + getHeight(), getX() + getWidth(), getY() + getHeight() + 1, getSkin().getInt("hovered_outline_color")));
                super.renderWidgets(matrices, mouseX, mouseY, delta);
            }
        };
        set.addWidget(new Label(12, widgetH / 2, Text.translated("setting." + setting.getName()), Alignment.CENTER_LEFT));

        Button reset = new Button(width - border - border - resetW, (widgetH - resetW) / 2, resetW, resetW, null, null);
        reset.setTooltip(Text.translated("gui.settings.reset.tooltip"));
        reset.setRenderBackground(false);
        reset.setActive(!setting.isDefault());
        reset.setIcon(RESET_ICON);

        switch (setting) {
            case Setting.Ints intSetting -> {
                TextField field = new TextField(x, 0, widgetW, widgetH);
                field.setText(intSetting.get());
                field.setListener(s -> {
                    try {
                        int value = Integer.parseInt(s);
                        intSetting.setTempValue(value);
                        field.setBorderColor((Integer) null);
                    } catch (NumberFormatException e) {
                        field.setBorderColor(Colors.RED);
                    }
                    reset.setActive(true);
                });
                reset.setAction(b -> {
                    intSetting.setTempValue(intSetting.getDefault());
                    field.setText(intSetting.getDefault());
                    reset.setActive(false);
                });
                set.addWidget(field);
            }
            case Setting.Floats floatSetting -> {
                TextField field = new TextField(x, 0, widgetW, widgetH);
                field.setText(floatSetting.get());
                field.setListener(s -> {
                    try {
                        float value = Float.parseFloat(s);
                        floatSetting.setTempValue(value);
                        field.setBorderColor((Integer) null);
                    } catch (NumberFormatException e) {
                        field.setBorderColor(Colors.RED);
                    }
                    reset.setActive(true);
                });
                reset.setAction(b -> {
                    floatSetting.setTempValue(floatSetting.getDefault());
                    field.setText(floatSetting.getDefault());
                    reset.setActive(false);
                });
                set.addWidget(field);
            }
            case Setting.List listSetting -> {
                ComboBox cb = new ComboBox(x, 0, widgetW, widgetH);
                cb.allowScrollSelect(false);
                Runnable resetComboBox = () -> {
                    cb.clearEntries();
                    String curr = listSetting.getTempValue() != null ? listSetting.getTempValue() : listSetting.get();
                    List<Pair<String, String>> entries = listSetting.getValuesSupplier().get();
                    entries.sort((a, b) -> a.second().compareToIgnoreCase(b.second()));
                    for (Pair<String, String> entry : entries) {
                        cb.addEntry(Text.translated(entry.second()), null, b -> {
                            listSetting.setTempValue(entry.first());
                            reset.setActive(true);
                        });
                        if (entry.first().equals(curr))
                            cb.setSelected(cb.getEntryCount() - 1);
                    }
                };
                resetComboBox.run();
                reset.setAction(b -> {
                    listSetting.setTempValue(listSetting.getDefault());
                    resetComboBox.run();
                    reset.setActive(false);
                });
                set.addWidget(cb);
            }
            case Setting.Enums enumSetting -> {
                ComboBox cb = new ComboBox(x, 0, widgetW, widgetH);
                cb.allowScrollSelect(false);
                Runnable resetComboBox = () -> {
                    cb.clearEntries();
                    String curr = enumSetting.getTempValue() != null ? enumSetting.getTempValue() : enumSetting.get();
                    for (Enum<?> entry : enumSetting.getEnumClass().getEnumConstants()) {
                        cb.addEntry(Text.of(entry.name()), null, b -> {
                            enumSetting.setTempValue(entry.name());
                            reset.setActive(true);
                        });
                        if (entry.name().equals(curr))
                            cb.setSelected(cb.getEntryCount() - 1);
                    }
                };
                resetComboBox.run();
                reset.setAction(b -> {
                    enumSetting.setTempValue(enumSetting.getDefault());
                    resetComboBox.run();
                    reset.setActive(false);
                });
                set.addWidget(cb);
            }
            case Setting.Strings stringSetting -> {
                TextField field = new TextField(x, 0, widgetW, widgetH);
                field.setText(stringSetting.get());
                field.setListener(str -> {
                    stringSetting.setTempValue(str);
                    reset.setActive(true);
                });
                reset.setAction(b -> {
                    stringSetting.setTempValue(stringSetting.getDefault());
                    field.setText(stringSetting.getDefault());
                    reset.setActive(false);
                });
                set.addWidget(field);
            }
            case Setting.Bools boolSetting -> {
                Switch sw = new Switch(0, 0, null) {
                    @Override
                    protected int getButtonWidth() {
                        return widgetW;
                    }
                    @Override
                    protected int getButtonHeight() {
                        return widgetH;
                    }
                };
                sw.setRightToLeft(true);
                sw.setPos(x, (widgetH - sw.getHeight()) / 2);
                sw.setToggled(boolSetting.get());
                sw.setAction(b -> {
                    boolSetting.setTempValue(sw.isToggled());
                    reset.setActive(true);
                });
                reset.setAction(b -> {
                    boolSetting.setTempValue(boolSetting.getDefault());
                    sw.setToggled(boolSetting.getDefault());
                    reset.setActive(false);
                });
                set.addWidget(sw);
            }
            case Setting.Ranges rangeSetting -> {
                Slider sl = new Slider(x, (widgetH - 8) / 2, widgetW);
                float min = rangeSetting.getMin(), max = rangeSetting.getMax();
                float range = max - min;
                sl.setAllowScroll(false);
                sl.setPercentage((rangeSetting.get() - min) / range);
                sl.setUpdateListener((f, i) -> {
                    rangeSetting.setTempValue(Math.lerp(min, max, f));
                    reset.setActive(true);
                });
                sl.setTooltipFunction((f, i) -> {
                    float t = rangeSetting.getTempValue() == null ? rangeSetting.get() : rangeSetting.getTempValue();
                    if (min == 0f && max == 1f)
                        return Text.of(String.format("%.0f%%", t * 100f));
                    else
                        return Text.of(String.format("%.2f", t));
                });
                reset.setAction(b -> {
                    rangeSetting.setTempValue(rangeSetting.getDefault());
                    sl.setPercentage((rangeSetting.getDefault() - min) / range);
                    reset.setActive(false);
                });
                set.addWidget(sl);
            }
            case Setting.IntRanges intRangeSetting -> {
                Slider sl = new Slider(x, (widgetH - 8) / 2, widgetW);
                int min = intRangeSetting.getMin(), max = intRangeSetting.getMax(), step = intRangeSetting.getStep();
                sl.setAllowScroll(false);
                sl.setMin(min);
                sl.setMax(max);
                sl.setValue(intRangeSetting.get());
                sl.setStepCount(step > 0 ? (max - min) / step : 0);
                sl.setUpdateListener((f, i) -> {
                    intRangeSetting.setTempValue(i);
                    reset.setActive(true);
                });
                reset.setAction(b -> {
                    intRangeSetting.setTempValue(intRangeSetting.getDefault());
                    sl.setValue(intRangeSetting.getDefault());
                    reset.setActive(false);
                });
                set.addWidget(sl);
            }
            case Setting.Keybind keybindSetting -> {
                keybindList.add(keybindSetting);
                Button butt = new Button(x, 0, widgetW, widgetH, keybindSetting.get().getKeyText(), b -> {
                    currentKeybind = keybindSetting;
                    keybindButton = b;
                    Keybind.KeyType keytype = currentKeybind.getTempType();
                    b.setMessage(Text.of("[ ").append(keytype == null ? keybindSetting.get().getKeyText() : keytype.getKeyText(keybindSetting.getTempKey(), keybindSetting.getTempMods())).append(" ]"));
                    keybindSetting.setTempValue(keybindSetting.get());
                    reset.setActive(true);
                });
                reset.setAction(b -> {
                    keybindSetting.setTempValue(keybindSetting.getDefault());
                    keybindSetting.setTemp(keybindSetting.getDefaultKey(), keybindSetting.getDefaultMods(), keybindSetting.getDefaultType(), keybindSetting.getDefaultJoystick());
                    butt.setMessage(keybindSetting.getDefaultType().getKeyText(keybindSetting.getDefaultKey(), keybindSetting.getDefaultMods()));
                    reset.setActive(false);
                });
                set.addWidget(butt);
            }
            default -> set.addWidget(new Button(x, 0, widgetW, widgetH, Text.of(setting.get()), b -> Toast.addToast(Text.translated("gui.settings.unknown_type"))));
        }

        set.addWidget(reset);
        container.addWidget(set);
    }

    protected void discardKeybind() {
        Keybind.KeyType keytype = currentKeybind.getTempType();
        keybindButton.setMessage(keytype == null ? currentKeybind.get().getKeyText() : keytype.getKeyText(currentKeybind.getTempKey(), currentKeybind.getTempMods()));
        if (keytype == null)
            currentKeybind.setTempValue(null);
        currentKeybind = null;
        keybindButton = null;
    }

    protected void checkKeybindConflict(int key, int mods, Keybind.KeyType type, int joystick) {
        List<Text> conflicts = new ArrayList<>();
        for (Setting.Keybind keybind : keybindList) {
            if (keybind == currentKeybind)
                continue;

            boolean hasTemp = keybind.getTempValue() != null;
            Keybind.KeyType otype = hasTemp ? keybind.getTempType() : keybind.get().getType();
            int okey = hasTemp ? keybind.getTempKey() : keybind.get().getKey();

            if (otype == type && okey == key)
                conflicts.add(Text.translated("setting." + keybind.getName()));
        }

        if (conflicts.isEmpty()) {
            currentKeybind.setTemp(key, mods, type, joystick);
            discardKeybind();
            return;
        }

        Text message = Text.translated("gui.settings.keybinds.conflict")
                .append("\n")
                .append(TextUtils.join(conflicts))
                .append("\n\n")
                .append(Text.translated("gui.settings.keybinds.conflict.accept"));

        ConfirmPopup.YesNo confirm = new ConfirmPopup.YesNo(message, bool -> {
            if (bool)
                currentKeybind.setTemp(key, mods, type, joystick);
            discardKeybind();
        });
        UIHelper.setPopup(0, 0, confirm);
        confirm.open();
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (currentKeybind == null || (popup != null && popup.isOpen()))
            return super.keyPress(key, scancode, action, mods);

        if (action != GLFW_RELEASE)
            return true;

        if (key == GLFW_KEY_ESCAPE) {
            discardKeybind();
            return true;
        }

        if (key == GLFW_KEY_BACKSPACE) {
            currentKeybind.setTemp(-1, 0, Keybind.KeyType.KEY, -1);
            discardKeybind();
            return true;
        }

        boolean scan = key == GLFW_KEY_UNKNOWN;
        checkKeybindConflict(scan ? scancode : key, mods, scan ? Keybind.KeyType.SCANCODE : Keybind.KeyType.KEY, -1);
        return true;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (currentKeybind == null || (popup != null && popup.isOpen()))
            return super.mousePress(button, action, mods);

        if (action != GLFW_RELEASE)
            return true;

        checkKeybindConflict(button, mods, Keybind.KeyType.MOUSE, -1);
        return true;
    }

    @Override
    public boolean gamepadButtonPress(int button, boolean pressed, int joystick) {
        if (currentKeybind == null || (popup != null && popup.isOpen()))
            return super.gamepadButtonPress(button, pressed, joystick);

        if (!pressed)
            return true;

        checkKeybindConflict(button, 0, Keybind.KeyType.GAMEPAD_BUTTON, joystick);
        return true;
    }

    @Override
    public boolean gamepadAxisMove(int axis, float value, int joystick, float lastValue) {
        if (currentKeybind == null || (popup != null && popup.isOpen()))
            return super.gamepadAxisMove(axis, value, joystick, lastValue);

        float deadzone = Settings.gamepadDeadzone.get();
        if (Math.abs(value) <= deadzone || Math.abs(lastValue) > deadzone)
            return true;

        checkKeybindConflict(axis, 0, Keybind.KeyType.GAMEPAD_AXIS, joystick);
        return true;
    }

    @Override
    public boolean xrButtonPress(int button, boolean pressed, int hand) {
        if (currentKeybind == null || (popup != null && popup.isOpen()))
            return super.xrButtonPress(button, pressed, hand);

        if (!pressed)
            return true;

        checkKeybindConflict(button, 0, Keybind.KeyType.XR_BUTTON, hand);
        return true;
    }

    @Override
    public boolean xrTriggerPress(int button, float value, int hand, float lastValue) {
        if (currentKeybind == null || (popup != null && popup.isOpen()))
            return super.xrTriggerPress(button, value, hand, lastValue);

        float deadzone = Settings.gamepadDeadzone.get();
        if (Math.abs(value) <= deadzone || Math.abs(lastValue) > deadzone)
            return true;

        checkKeybindConflict(button, 0, Keybind.KeyType.XR_TRIGGER, hand);
        return true;
    }

    protected static class Category {
        public final List<Setting<?>> settings = new ArrayList<>();
        public final Map<String, Category> subcategories = new LinkedHashMap<>();
    }
}