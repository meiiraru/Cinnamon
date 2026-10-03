package cinnamon.gui.screens.world;

import cinnamon.gui.GUISkin;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.Switch;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.render.MatrixStack;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.CircularQueue;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.world.terrain.Terminal;

public class TerminalScreen extends Screen {

    public static final Resource SKIN = new Resource("data/gui_skins/terminal.json");

    protected final CircularQueue<Text> outputQueue = new CircularQueue<>(5);
    protected final Label outputText = new Label(0, 0, Text.empty());
    protected int maxOutputWidth = 0;

    protected final Terminal terminal;

    public TerminalScreen(Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void init() {
        super.init();
        this.mainContainer.setSkin(SKIN);

        //title
        Label label = new Label(width / 2, 32, Text.translated("gui.terminal").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER);
        addWidget(label);

        //command field
        Label commandLabel = new Label(32, 64, Text.translated("gui.terminal.command").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_LEFT);
        addWidget(commandLabel);

        TextField field = new TextField(32, commandLabel.getY() + commandLabel.getHeight() + 4, width - 64, 20);
        field.setHintText(Text.translated("gui.terminal.type_command"));
        field.setText(terminal.getCommandString());
        addWidget(field);

        //switches
        ContainerGrid switches = new ContainerGrid(width - 32, field.getY() + field.getHeight() + 8, 4);
        addWidget(switches);

        Switch enable = new Switch(0, 0, Text.translated("gui.terminal.enable_terminal").withStyle(Style.EMPTY.outlined(true)));
        enable.setToggled(terminal.isActive());
        switches.addWidget(enable);

        Switch chatOutput = new Switch(0, 0, Text.translated("gui.terminal.chat_output").withStyle(Style.EMPTY.outlined(true)));
        chatOutput.setToggled(terminal.isSendingOutputToChat());
        switches.addWidget(chatOutput);

        Button clearOutput = new Button(0, 0, 85, 16, Text.translated("gui.terminal.clear_output"), _ -> {
            outputQueue.clear();
            outputText.setText(Text.empty());
        });
        switches.addWidget(clearOutput);

        switches.setX(width - 32 - switches.getWidth());

        maxOutputWidth = switches.getX() - 32 - 4;

        //last output
        Label lastOutputLabel = new Label(32, field.getY() + field.getHeight() + 8, Text.translated("gui.terminal.last_output").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_LEFT);
        addWidget(lastOutputLabel);

        outputText.setPos(32, lastOutputLabel.getY() + lastOutputLabel.getHeight() + 4);
        addWidget(outputText);

        //apply / cancel
        ContainerGrid grid = new ContainerGrid(width / 2, height - 32, 4, 2);
        grid.setAlignment(Alignment.BOTTOM_CENTER);
        addWidget(grid);

        Button cancel = new Button(0, 0, 60, 20, Text.translated("gui.exit"), _ -> close());
        grid.addWidget(cancel);

        Button apply = new Button(0, 0, 60, 20, Text.translated("gui.confirm"), _ -> {
            terminal.setCommandString(field.getText());
            terminal.setActive(enable.isToggled());
            terminal.setSendOutputToChat(chatOutput.isToggled());
        });
        grid.addWidget(apply);
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        renderSolidBackground(GUISkin.of(SKIN).getInt("world_screen_bg_color"));
    }

    public void addOutputEntry(Text output) {
        outputQueue.add(output);

        Text text = Text.empty();
        for (int i = outputQueue.size() - 1; i >= 0; i--)
            text = text
                    .append(TextUtils.addEllipsis(Text.empty().withStyle(Style.EMPTY.guiSkin(SKIN)).append(outputQueue.get(i)), maxOutputWidth))
                    .append(Text.of("\n"));

        outputText.setText(text);
    }
}
