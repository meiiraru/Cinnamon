package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.gui.widgets.types.*;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.settings.Settings;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.world.Hud;

import static cinnamon.Client.LOGGER;

public class WidgetTestScreen extends ParentedScreen {

    private int clicks = 0;
    private Label clicksLabel;

    public WidgetTestScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        clicksLabel = new Label(width / 2, height - 4, Text.of(clicks));
        clicksLabel.setAlignment(Alignment.BOTTOM_CENTER);
        clicksLabel.setTooltip(Text.of("Voided Clicks"));
        clicksLabel.setPopup(new ContextMenu()
                .addAction(Text.of("Reset"), Text.of("Reset"), button -> clicks = 0)
                .addDivider()
                .addAction(Text.of("Meow"), Text.of("~ :3"), button -> System.out.println("meow"))
        );

        ContainerGrid grid = new ContainerGrid(4, 4, 4);

        //button
        ContainerGrid buttons = new ContainerGrid(0, 0, 4, 2);
        grid.addWidget(buttons);

        Button b = new Button(0, 0, 60, 12, Text.of("Button"), button -> LOGGER.info("button!"));
        buttons.addWidget(b);

        ContextMenu ctx4 = new ContextMenu();
        for (int i = 0; i < 100; i++) {
            int ii = i;
            ctx4.addAction(Text.of(i), Text.of(i), button -> LOGGER.info(ii));
        }

        b.setPopup(new ContextMenu()
                .addAction(Text.of("Meow 1"), Text.of("Meow 1"), button -> LOGGER.info("Meow 1"))
                .addDivider()
                .addAction(Text.of("Meow 2"), Text.of("Meow 2"), button -> LOGGER.info("Meow 2"))
                .addDivider()
                .addAction(Text.of("Meow 3"), Text.of("Meow 3"), button -> LOGGER.info("Meow 3"))
                .addSubMenu(Text.of("I"), new ContextMenu()
                        .addAction(Text.of("A"), Text.of("A"), button -> LOGGER.info("a"))
                        .addAction(Text.of("B"), Text.of("B"), button -> LOGGER.info("b"))
                        .addAction(Text.of("C"), Text.of("C"), button -> LOGGER.info("c"))
                        .addSubMenu(Text.of("II"), new ContextMenu()
                                .addAction(Text.of("1"), Text.of("1"), button -> LOGGER.info("II. 1"))
                                .addAction(Text.of("11"), Text.of("11"), button -> LOGGER.info("II. 11"))
                                .addAction(Text.of("111"), Text.of("111"), button -> LOGGER.info("II. 111"))
                                .addSubMenu(Text.of("III"), ctx4)
                        )
                )
        );

        Button disabledButt = new Button(0, 0, 60, 12, Text.of("Disabled"), button -> LOGGER.info("Disabled"));
        disabledButt.setActive(false);
        buttons.addWidget(disabledButt);

        //label
        Label l = new Label(0, 0, Text.of("Progress: "));
        grid.addWidget(l);

        //progress bar
        ContainerGrid pbGrid = new ContainerGrid(0, 0, 4, 2);
        ProgressBar pb = new ProgressBar(0, 0, 60, 12, 0f);
        pbGrid.addWidget(pb);

        //round progress bar
        CircularProgressBar cpb = new CircularProgressBar(0, 0, 0f);
        pbGrid.addWidget(cpb);

        grid.addWidget(pbGrid);

        //selection box
        ComboBox cb = new ComboBox(0, 0, 60, 12);
        grid.addWidget(cb);

        cb
                .addEntry(Text.of("Entry 1"), Text.of("1"), button -> LOGGER.info("1"))
                .addDivider()
                .addEntry(Text.of("Entry 2"), Text.of("2"), button -> LOGGER.info("2"))
                .addDivider()
                .addEntry(Text.of("Entry 3"), Text.of("3"), button -> LOGGER.info("3"));

        //selection 2
        ComboBox cb2 = new ComboBox(0, 0, 60, 12);
        grid.addWidget(cb2);

        for (int i = 0; i < 100; i++)
            cb2.addEntry(Text.of(i), Text.of(i), null);

        //selection 3
        ComboBox cb3 = new ComboBox(0, 0, 60, 12);
        cb3.setActive(false);
        cb3.addEntry(Text.of("Disabled"), Text.of("Disabled"), null);
        grid.addWidget(cb3);

        //slider
        Slider s = new Slider(0, 0, 60);
        grid.addWidget(s);

        s.setUpdateListener((f, i) -> {
            l.setText(Text.of("Progress: " + s.getStepIndex()));
            pb.setProgress(f);
            cpb.setProgress(f);
        });
        s.setMax(1000);
        s.setPercentage((float) Math.random());

        b.setAction(button -> s.setStepCount(s.getStepCount() + 1));

        //text fields

        //normal text field
        int tfw = Math.min(Math.round(width / 2f - 8), 180);
        TextField tf1 = new TextField(0, 0, tfw, 16);
        grid.addWidget(tf1);

        tf1.setHintText(Text.of("Text Field"));
        tf1.setListener(s1 -> tf1.setTextStyle(tf1.getTextStyle().color(Colors.randomRainbow())));
        tf1.setTextStyle(Style.EMPTY.guiStyle(Hud.HUD_STYLE));

        TextField tf2 = new TextField(0, 0, tfw, 16);
        grid.addWidget(tf2);

        tf2.setHintText(Text.of("Disabled Text Field"));
        tf2.setActive(false);
        tf2.setTextStyle(Style.EMPTY.guiStyle(Hud.HUD_STYLE));

        ContainerGrid password = new ContainerGrid(0, 0, 4, 2);
        grid.addWidget(password);

        TextField tf4 = new TextField(0, 0, tfw, 16);
        tf4.setHintText(Text.of("Password..."));
        tf4.setPassword(true);
        password.addWidget(tf4);

        boolean[] showing = {false};
        Resource password1 = new Resource("textures/gui/icons/show_password.png");
        Resource password2 = new Resource("textures/gui/icons/hide_password.png");
        Button viewPassword = new Button(0, 0, 16, 16, null, button -> {
            showing[0] = !showing[0];
            tf4.setPassword(!showing[0]);
            button.setIcon(showing[0] ? password2 : password1);
            button.setTooltip(Text.of(showing[0] ? "Hide Password" : "Show Password"));
        });
        viewPassword.setIcon(password1);
        viewPassword.setTooltip(Text.of("Show Password"));
        password.addWidget(viewPassword);

        //tooltip test
        SelectableWidget empty = new SelectableWidget(0, 0, 10, 10) {
            @Override
            public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                setPos(mouseX - getWidth() / 2, mouseY - getHeight() / 2);
                VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x88 << 24));
            }
            @Override
            public GUIListener mouseMove(int x, int y) {
                super.mouseMove(x, y);
                return null;
            }
        };
        empty.setTooltip(Text.of("Tooltip!"));

        //checkbox
        Checkbox ckb = new Checkbox(0, 0, Text.of("Checkbox"));
        ckb.setAction(button -> {
            if (ckb.isToggled())
                this.addWidgetOnTop(empty);
            else
                this.removeWidget(empty);
        });
        grid.addWidget(ckb);

        Checkbox ckb2 = new Checkbox(0, 0, Text.of("No Checkbox :("));
        ckb2.setActive(false);
        grid.addWidget(ckb2);

        //toast 1
        Button toast1 = new Button(0, 0, 60, 12, Text.of("Toast 1"), button -> Toast.addToast(Text.of("Toast 1")).style(button.getStyleRes()));
        grid.addWidget(toast1);

        //toast 2
        Button toast2 = new Button(0, 0, 60, 12, Text.of("Toast 2"), button -> Toast.addToast(Text.of("Multi-line\nToast :3")).style(button.getStyleRes()).type(Toast.ToastType.WARN));
        grid.addWidget(toast2);

        //toast 3
        Button toast3 = new Button(0, 0, 60, 12, Text.of("Toast 3"), button -> Toast.addToast(Text.of("Oopsie daisy")).style(button.getStyleRes()).type(Toast.ToastType.ERROR).color(Colors.randomRainbow().rgba));
        grid.addWidget(toast3);

        //right panel
        ContainerGrid grid2 = new ContainerGrid(width - 4, 4, 4);
        grid2.setAlignment(Alignment.TOP_RIGHT);

        //button grid
        ContainerGrid buttonsGrid = new ContainerGrid(0, 0, 2, 3);
        grid2.addWidget(buttonsGrid);

        Label rightLabel = new Label(0, 0, Text.of("Alignment:\n" + buttonsGrid.getAlignment().name()));
        grid2.addWidget(rightLabel);

        for (int i = 0; i < 9; i++) {
            int x = i % 3 + 1;
            int y = i / 3 + 1;
            final int fi = i;
            Button btx = new Button(0, 0, 30 + y * 3, 12 + x * 3, Text.of(y + "-" + x), button -> {
                buttonsGrid.setAlignment(Alignment.values()[fi]);
                rightLabel.setText(Text.of("Alignment:\n" + buttonsGrid.getAlignment().name()));
            });
            buttonsGrid.addWidget(btx);
        }

        //vertical stuff
        ContainerGrid vertical = new ContainerGrid(0, 0, 4, 100);
        grid2.addWidget(vertical);

        //vertical slider
        Slider s2 = new Slider(0, 0, 40);
        vertical.addWidget(s2);

        s2.setMax((int) client.window.maxGuiScale);
        s2.setStepCount((int) client.window.maxGuiScale + 1);
        s2.setChangeListener((f, i) -> {
            Settings.guiScale.set((float) i);
            Settings.save();
            client.windowResize(client.window.width, client.window.height);
        });
        s2.setUpdateListener((f, i) -> s2.setColor(Colors.RAINBOW[i % Colors.RAINBOW.length]));
        s2.updateValue((int) client.window.guiScale);
        s2.setVertical(true);

        Slider s3 = new Slider(0, 0, 40);
        s3.setVertical(true);
        vertical.addWidget(s3);

        //disabled slider
        Slider s4 = new Slider(0, 0, 40);
        s4.setVertical(true);
        s4.setActive(false);
        vertical.addWidget(s4);

        //scrollbar
        Scrollbar bar = new Scrollbar(0, 0, 40);
        vertical.addWidget(bar);

        //disabled scrollbar
        Scrollbar bar1 = new Scrollbar(0, 0, 40);
        bar1.setActive(false);
        vertical.addWidget(bar1);

        //vertical scrollbar
        Scrollbar bar2 = new Scrollbar(0, 0, 40);
        bar2.setVertical(false);
        grid2.addWidget(bar2);

        //font test
        grid2.addWidget(new Label(0, 0, Text.empty()
                .append("Font Test:\n")
                .append(TextUtils.join(
                        TextUtils.warpToWidth(Text.empty()
                                .append(Text.of("one ")  .withStyle(Style.EMPTY.bold(true)))
                                .append(Text.of("two ")  .withStyle(Style.EMPTY.italic(true)))
                                .append(Text.of("three ").withStyle(Style.EMPTY.underlined(true)))
                                .append(Text.of("four ") .withStyle(Style.EMPTY.obfuscated(true)))
                                .append(Text.of("five ") .withStyle(Style.EMPTY.strikethrough(true)))
                                .append(Text.of("six ")  .withStyle(Style.EMPTY.background(true)))
                                .append(Text.of("seven ").withStyle(Style.EMPTY.shadow(true)))
                                .append(Text.of("eight") .withStyle(Style.EMPTY.outlined(true))),
                                96),
                        Text.of("\n"))
                )
        ));

        //color picker
        ColorPicker cp = new ColorPicker(0, 0, 16, 16);
        cp.setColor(Colors.randomRainbow());
        grid2.addWidget(cp);

        //add lists
        addWidget(grid);
        addWidget(grid2);
        addWidget(clicksLabel);
        super.init();
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
        if (sup) return true;
        clicksLabel.setText(Text.of(++clicks));
        return false;
    }
}
