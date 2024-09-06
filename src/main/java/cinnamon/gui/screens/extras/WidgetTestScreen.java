package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.*;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;

import static cinnamon.Client.LOGGER;

public class WidgetTestScreen extends ParentedScreen {

    private int clicks = 0;
    private Label clicksLabel;

    public WidgetTestScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        clicksLabel = new Label(width / 2, (int) (height - font.lineHeight), Text.of(clicks), font);
        clicksLabel.setAlignment(Alignment.CENTER);

        ContainerGrid grid = new ContainerGrid(4, 4, 4);

        //button
        Button b = new Button(0, 0, 60, 12, Text.of("Button"), button -> LOGGER.info("button!"));
        grid.addWidget(b);

        ContextMenu ctx = new ContextMenu()
                .addAction(Text.of("Meow 1"), Text.of("Meow 1"), button -> LOGGER.info("1"))
                .addDivider()
                .addAction(Text.of("Meow 2"), Text.of("Meow 2"), button -> LOGGER.info("2"))
                .addDivider()
                .addAction(Text.of("Meow 3"), Text.of("Meow 3"), button -> LOGGER.info("3"));

        ContextMenu ctx2 = new ContextMenu()
                .addAction(Text.of("A"), Text.of("A"), button -> LOGGER.info("a"))
                .addAction(Text.of("B"), Text.of("B"), button -> LOGGER.info("b"))
                .addAction(Text.of("C"), Text.of("C"), button -> LOGGER.info("c"));

        ContextMenu ctx3 = new ContextMenu()
                .addAction(Text.of("1"), Text.of("1"), button -> LOGGER.info("1"))
                .addAction(Text.of("11"), Text.of("11"), button -> LOGGER.info("11"))
                .addAction(Text.of("111"), Text.of("111"), button -> LOGGER.info("111"));

        ctx2.addDivider().addSubMenu(Text.of("||"), ctx3);
        ctx.addDivider().addSubMenu(Text.of("Purr~"), ctx2);

        b.setPopup(ctx);

        //label
        Label l = new Label(0, 0, Text.of("Label"), font);
        grid.addWidget(l);

        //progress bar
        ProgressBar pb = new ProgressBar(0, 0, 60, 12, 0f);
        pb.setColor(Colors.PINK);
        grid.addWidget(pb);

        //selection box
        SelectionBox sb = new SelectionBox(0, 0, 60, 12);
        grid.addWidget(sb);

        sb
                .addEntry(Text.of("Entry 1"), Text.of("1"), button -> LOGGER.info("1"))
                .addDivider()
                .addEntry(Text.of("Entry 2"), Text.of("2"), button -> LOGGER.info("2"))
                .addDivider()
                .addEntry(Text.of("Entry 3"), Text.of("3"), button -> LOGGER.info("3"));

        //slider
        Slider s = new Slider(0, 0, 60);
        grid.addWidget(s);

        s.setColor(Colors.PINK);
        s.setChangeListener((f, i) -> {
            l.setText(Text.of("Label " + s.getStepIndex()));
            pb.setProgress(f);
        });
        s.setMax(1000);
        s.setValue((int) (Math.random() * 500) + 250);

        b.setAction(button -> s.setStepCount(s.getStepCount() + 1));

        //text fields

        //normal text field
        int tfw = Math.min(Math.round(width / 2f - 8), 180);
        TextField tf1 = new TextField(0, 0, tfw, 14, font);
        grid.addWidget(tf1);

        tf1.setHintText("Text Field");
        tf1.setListener(s1 -> tf1.setStyle(tf1.getStyle().color(Colors.randomRainbow())));

        ContainerGrid password = new ContainerGrid(0, 0, 4, 2);
        grid.addWidget(password);

        TextField tf4 = new TextField(0, 0, tfw, 14, font);
        tf4.setHintText("Password...");
        tf4.setPassword(true);
        password.addWidget(tf4);

        Button viewPassword = new Button(0, 0, 14, 14, Text.of("\u26F6"), button -> tf4.setPassword(!button.isHolding()));
        viewPassword.setRunOnHold(true);
        password.addWidget(viewPassword);

        //toggle button
        ToggleButton tb = new ToggleButton(0, 0, Text.of("Toggle Button"));
        grid.addWidget(tb);

        //toast 1
        Button toast1 = new Button(0, 0, 60, 12, Text.of("Toast 1"), button -> Toast.addToast(Text.of("Toast 1"), font));
        grid.addWidget(toast1);

        //toast 2
        Button toast2 = new Button(0, 0, 60, 12, Text.of("Toast 2"), button -> Toast.addToast(Text.of("Multi-line\nToast :3"), font).type(Toast.ToastType.WARN));
        grid.addWidget(toast2);

        //toast 3
        Button toast3 = new Button(0, 0, 60, 12, Text.of("Toast 3"), button -> Toast.addToast(Text.of("Oopsie daisy"), font).type(Toast.ToastType.ERROR).color(Colors.randomRainbow().rgba));
        grid.addWidget(toast3);

        //right panel
        ContainerGrid grid2 = new ContainerGrid(width - 4, 4, 4);
        grid2.setAlignment(Alignment.RIGHT);

        //button grid
        ContainerGrid buttonsGrid = new ContainerGrid(0, 0, 2, 3);
        grid2.addWidget(buttonsGrid);

        for (int i = 0; i < 9; i++) {
            int x = i % 3 + 1;
            int y = i / 3 + 1;
            Button btx = new Button(0, 0, 30, 12, Text.of(y + "-" + x), button -> LOGGER.info("{} {}", y, x));
            buttonsGrid.addWidget(btx);
        }

        Label rightLabel = new Label(0, 0, Text.of("Some text\nNo Alignment"), font);
        buttonsGrid.addWidget(rightLabel);

        //vertical stuff
        ContainerGrid vertical = new ContainerGrid(0, 0, 4, 100);
        grid2.addWidget(vertical);

        //vertical slider
        Slider s2 = new Slider(0, 0, 40);
        vertical.addWidget(s2);

        s2.setMin(1);
        s2.setMax(5);
        s2.setStepCount(5);
        s2.setChangeListener((f, i) -> s2.setColor(Colors.values()[i]));
        s2.setPercentage((float) Math.random());
        s2.setVertical(true);

        //scrollbar
        Scrollbar bar = new Scrollbar(0, 0, 40);
        vertical.addWidget(bar);

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
