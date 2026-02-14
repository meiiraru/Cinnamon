package cinnamon.scripting;

import cinnamon.gui.screens.world.ChatScreen;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A chat screen that executes Lua code instead of sending chat messages.
 * Commands starting with / are still passed to the command parser.
 * Everything else is executed as Lua code, with output shown as chat messages.
 */
public class LuaChatScreen extends ChatScreen {

    private final LuaEngine engine;

    public LuaChatScreen(LuaEngine engine) {
        this.engine = engine;
        engine.setOutputConsumer(text -> addMessage(Text.of(text).withStyle(Style.EMPTY.color(Colors.WHITE))));
    }

    @Override
    public void init() {
        super.init();
        field.setHintText(Text.of("Enter Lua code or /command...").withStyle(Style.EMPTY.color(Colors.LIGHT_GRAY)));
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER)) {
            String s = field.getText();
            setFieldText("");

            if (!s.isBlank()) {
                // Store in history
                sentMessages.add(s);
                sentIndex = -1;

                // Commands starting with / go to the normal command parser
                if (s.startsWith("/")) {
                    close();
                    cinnamon.commands.CommandParser.parseCommand(client.world.player, s.substring(1));
                    return true;
                }

                // Show input in chat
                addMessage(Text.of("> " + s).withStyle(Style.EMPTY.color(Colors.YELLOW)));

                // Execute as Lua
                String result = engine.execute(s);
                if (result != null) {
                    Colors color = result.startsWith("Error:") ? Colors.RED : Colors.LIME;
                    addMessage(Text.of(result).withStyle(Style.EMPTY.color(color)));
                }
            }

            close();
            return true;
        }

        return super.keyPress(key, scancode, action, mods);
    }
}
