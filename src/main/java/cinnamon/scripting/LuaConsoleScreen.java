package cinnamon.scripting;

import cinnamon.Client;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.world.Hud;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * In-game Lua console screen. Toggled with backtick (`) key.
 * Provides a text input field and scrollable output history.
 */
public class LuaConsoleScreen extends Screen {

    private static final int MAX_OUTPUT_LINES = 200;
    private static final int VISIBLE_LINES = 16;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 6;
    private static final int CONSOLE_MARGIN = 4;

    private final LuaEngine engine;
    private final List<ConsoleLine> outputLines = new ArrayList<>();
    private final List<String> inputHistory = new ArrayList<>();
    private int historyIndex = -1;
    private int scrollOffset = 0;

    private TextField inputField;

    public LuaConsoleScreen(LuaEngine engine) {
        this.engine = engine;
        engine.setOutputConsumer(text -> addOutput(text, Colors.WHITE));
    }

    @Override
    public void init() {
        // Input field at bottom
        int fieldWidth = width - CONSOLE_MARGIN * 2 - PADDING * 2;
        int fieldHeight = 16;
        int fieldY = height - CONSOLE_MARGIN - PADDING - fieldHeight;
        int fieldX = CONSOLE_MARGIN + PADDING;

        inputField = new TextField(fieldX, fieldY, fieldWidth, fieldHeight);
        inputField.setHintText(Text.of("Enter Lua code...").withStyle(Style.EMPTY.color(Colors.LIGHT_GRAY)));
        inputField.setCharLimit(1000);
        inputField.setTextOnly(true);

        inputField.setEnterListener(field -> {
            String text = field.getText().strip();
            if (!text.isEmpty()) {
                executeInput(text);
                field.setText("");
                historyIndex = -1;
            }
        });

        addWidget(inputField);
        focusWidget(inputField);

        super.init();
    }

    private void executeInput(String input) {
        // Add to history
        inputHistory.addFirst(input);
        if (inputHistory.size() > 100) {
            inputHistory.removeLast();
        }

        // Show input in console
        addOutput("> " + input, Colors.YELLOW);

        // Execute
        String result = engine.execute(input);
        if (result != null) {
            if (result.startsWith("Error:")) {
                addOutput(result, Colors.RED);
            } else {
                addOutput(result, Colors.LIME);
            }
        }

        // Auto-scroll to bottom
        scrollOffset = 0;
    }

    private void addOutput(String text, Colors color) {
        // Split multiline output
        String[] lines = text.split("\n");
        for (String line : lines) {
            outputLines.add(new ConsoleLine(line, color));
            if (outputLines.size() > MAX_OUTPUT_LINES) {
                outputLines.removeFirst();
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Client c = Client.getInstance();
        Camera camera = c.camera;

        int consoleX = CONSOLE_MARGIN;
        int consoleY = CONSOLE_MARGIN;
        int consoleW = width - CONSOLE_MARGIN * 2;
        int consoleH = height - CONSOLE_MARGIN * 2;

        // Background
        VertexConsumer.MAIN.consume(
                GeometryHelper.rectangle(matrices, consoleX, consoleY, consoleX + consoleW, consoleY + consoleH, 0xCC000000)
        );

        // Title bar
        int titleBarHeight = 16;
        VertexConsumer.MAIN.consume(
                GeometryHelper.rectangle(matrices, consoleX, consoleY, consoleX + consoleW, consoleY + titleBarHeight, 0xFF1A1A2E)
        );

        Text.of("Lua Console")
                .withStyle(Style.EMPTY.outlined(true).guiStyle(Hud.HUD_STYLE).color(Colors.CYAN))
                .render(VertexConsumer.MAIN, matrices, consoleX + PADDING, consoleY + 3, Alignment.TOP_LEFT);

        Text.of("Press ` to close")
                .withStyle(Style.EMPTY.outlined(true).guiStyle(Hud.HUD_STYLE).color(Colors.DARK_GRAY))
                .render(VertexConsumer.MAIN, matrices, consoleX + consoleW - PADDING, consoleY + 3, Alignment.TOP_RIGHT);

        // Output area
        int outputY = consoleY + titleBarHeight + PADDING;
        int outputEndY = inputField.getY() - PADDING;
        int maxVisibleLines = (outputEndY - outputY) / LINE_HEIGHT;

        // Separator above input
        VertexConsumer.MAIN.consume(
                GeometryHelper.rectangle(matrices, consoleX + PADDING, inputField.getY() - 2, consoleX + consoleW - PADDING, inputField.getY() - 1, 0xFF333344)
        );

        // Render output lines (scrollable)
        int startIdx = Math.max(0, outputLines.size() - maxVisibleLines - scrollOffset);
        int endIdx = Math.max(0, outputLines.size() - scrollOffset);

        int lineY = outputY;
        for (int i = startIdx; i < endIdx && lineY < outputEndY; i++) {
            ConsoleLine line = outputLines.get(i);
            Text.of(line.text)
                    .withStyle(Style.EMPTY.guiStyle(Hud.HUD_STYLE).color(line.color))
                    .render(VertexConsumer.MAIN, matrices, consoleX + PADDING, lineY, Alignment.TOP_LEFT);
            lineY += LINE_HEIGHT;
        }

        VertexConsumer.finishAllBatches(camera);

        // Render widgets (text field)
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            switch (key) {
                case GLFW_KEY_GRAVE_ACCENT, GLFW_KEY_ESCAPE -> {
                    close();
                    return true;
                }
                case GLFW_KEY_UP -> {
                    navigateHistory(1);
                    return true;
                }
                case GLFW_KEY_DOWN -> {
                    navigateHistory(-1);
                    return true;
                }
                case GLFW_KEY_PAGE_UP -> {
                    scrollOffset = Math.min(scrollOffset + VISIBLE_LINES / 2, Math.max(0, outputLines.size() - VISIBLE_LINES));
                    return true;
                }
                case GLFW_KEY_PAGE_DOWN -> {
                    scrollOffset = Math.max(0, scrollOffset - VISIBLE_LINES / 2);
                    return true;
                }
            }
        }
        return super.keyPress(key, scancode, action, mods);
    }

    @Override
    public boolean scroll(double x, double y) {
        if (y > 0) {
            scrollOffset = Math.min(scrollOffset + 3, Math.max(0, outputLines.size() - VISIBLE_LINES));
        } else if (y < 0) {
            scrollOffset = Math.max(0, scrollOffset - 3);
        }
        return true;
    }

    private void navigateHistory(int direction) {
        if (inputHistory.isEmpty()) return;

        historyIndex += direction;
        historyIndex = Math.max(-1, Math.min(historyIndex, inputHistory.size() - 1));

        if (historyIndex >= 0) {
            inputField.setText(inputHistory.get(historyIndex));
            inputField.setCursorToEnd();
        } else {
            inputField.setText("");
        }
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    // -- Console line record --

    private record ConsoleLine(String text, Colors color) {}
}
