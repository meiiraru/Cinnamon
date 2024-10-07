package cinnamon.gui.screens.world;

import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class ChatScreen extends Screen {

    private static final int MAX_MESSAGES = 100;
    private static final int RECENT_MESSAGES = 8;

    private final List<Message> messages = new ArrayList<>();

    private TextField field;
    private String fieldMsg = "";

    @Override
    public void init() {
        super.init();
        int fh = (int) (font.lineHeight + 2);
        field = new TextField(0, height - fh - 20, width, fh, font);
        field.setHintText(Text.of("Type a message...").withStyle(Style.EMPTY.color(Colors.LIGHT_GRAY)));
        field.setTextOnly(true);
        field.setText(fieldMsg);
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
        VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, field.getX(), field.getY(), field.getX() + field.getWidth(), field.getY() + field.getHeight(), 0x80000000));
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER)) {
            String s = field.getText();
            field.setText(fieldMsg = "");
            if (!s.isBlank()) {
                addMessage(s);
                //close();
                return true;
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
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

    private static class Message {

        private final long addedTime;
        private final Text text;

        private Message(long addedTime, Text text) {
            this.addedTime = addedTime;
            this.text = text;
        }
    }
}
