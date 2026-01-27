package cinnamon.gui.screens.world;

import cinnamon.commands.CommandParser;
import cinnamon.gui.GUIStyle;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class ChatScreen extends Screen {

    public static final int MAX_MESSAGES = 100;
    public static final int RECENT_MESSAGES = 8;

    protected static final List<Message> messages = new ArrayList<>();
    protected static final List<String> sentMessages = new ArrayList<>();

    protected final TextField field;
    protected int sentIndex = -1;
    protected String backupText = "";

    public ChatScreen() {
        //create text field
        field = new TextField(0, 0, 0, 0);
        field.setHintText(Text.translated("gui.chat.type_message").withStyle(Style.EMPTY.color(Colors.LIGHT_GRAY)));
        field.setTextOnly(true);
    }

    @Override
    public void init() {
        super.init();

        //update field dimensions
        int fh = (int) (GUIStyle.getDefault().getFont().lineHeight + 2);
        field.setPos(0, height - fh - 20);
        field.setDimensions(width, fh);

        //add and focus field
        addWidget(field);
        this.focusWidget(field);
    }

    @Override
    protected void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render messages before children


        super.renderChildren(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        //super.renderBackground(matrices, delta);
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, field.getX(), field.getY(), field.getX() + field.getWidth(), field.getY() + field.getHeight(), -UIHelper.getDepthOffset(), 0x80000000));
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            switch (key) {
                //send the current message
                case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                    //clear the field
                    String s = field.getText();
                    setFieldText("");

                    //close the screen
                    close();

                    if (!s.isBlank()) {
                        //try to send a command
                        if (s.startsWith("/"))
                            CommandParser.parseCommand(client.world.player, s.substring(1));
                        //otherwise send as a chat message
                        else
                            addMessage(s);

                        //store the message
                        sentMessages.add(s);
                        return true;
                    }
                }
                //go to previous message
                case GLFW_KEY_UP -> {
                    //no messages in history, so do nothing
                    if (sentMessages.isEmpty())
                        return true;

                    //if we still have older messages
                    if (sentIndex < sentMessages.size() - 1) {
                        //store the current text if we are at the first message
                        if (sentIndex == -1)
                            backupText = field.getText();

                        //increase the index to go to an older message
                        sentIndex++;
                    }

                    //set the text to the selected message
                    setFieldText(sentMessages.get(sentMessages.size() - 1 - sentIndex));
                    return true;
                }
                //go to next message in history
                case GLFW_KEY_DOWN -> {
                    //no messages in history, so do nothing
                    if (sentMessages.isEmpty())
                        return true;

                    //if we have newer messages
                    if (sentIndex > 0) {
                        //decrease the index to go to a newer message
                        sentIndex--;
                        setFieldText(sentMessages.get(sentMessages.size() - 1 - sentIndex));
                    } else {
                        //going further than the newer message, reset to the backup text
                        sentIndex = -1;
                        setFieldText(backupText);
                    }
                    return true;
                }
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
        //always focus the field
        if (action == GLFW_PRESS)
            this.focusWidget(field);
        return sup;
    }

    public void addMessage(String msg) {
        addMessage(Text.of(msg));
    }

    public void addMessage(Text msg) {
        messages.add(new Message(client.ticks, msg));
    }

    public void setFieldText(String text) {
        field.setText(text);
        field.setCursorToEnd();
    }

    protected static class Message {

        private final long addedTime;
        private final Text text;

        private Message(long addedTime, Text text) {
            this.addedTime = addedTime;
            this.text = text;
        }
    }
}
