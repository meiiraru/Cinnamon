package cinnamon.gui.screens.world;

import cinnamon.commands.CommandParser;
import cinnamon.gui.GUIStyle;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.messages.Message;
import cinnamon.messages.MessageCategory;
import cinnamon.messages.MessageManager;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class ChatScreen extends Screen {

    public static final int MAX_SENT_HISTORY = 100;
    protected static final List<String> sentMessages = new CircularQueue<>(MAX_SENT_HISTORY);

    protected final List<Text> messagesToRender = new ArrayList<>();
    protected final TextField field;
    protected int sentIndex = -1;
    protected String backupText = "";
    protected int maxChatWidth, maxChatHeight, chatHeight, chatDiff, scrollMessages;
    protected float scroll = 1f;
    private Message lastMessage = null;
    protected MessageCategory selectedCategory = null;

    public ChatScreen() {
        //create text field
        field = new TextField(0, 0, 0, 0);
        field.setHintText(Text.translated("gui.chat.type_message").withStyle(MessageManager.DEFAULT_STYLE.color(Colors.LIGHT_GRAY)));
        field.setTextStyle(MessageManager.DEFAULT_STYLE);
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

        maxChatWidth = 350;
        maxChatHeight = field.getY() - 4 - 40;

        lastMessage = null;
        scroll = 1f;
    }

    @Override
    public void tick() {
        //fetch message update
        List<Message> messages = MessageManager.getMessages();
        Message last = messages.isEmpty() ? null : messages.getLast();
        if (lastMessage != last) {
            forceUpdate(messages);
            lastMessage = last;
        }

        super.tick();
    }

    @Override
    protected void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //field background
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, field.getX(), field.getY(), field.getX() + field.getWidth(), field.getY() + field.getHeight(), -UIHelper.getDepthOffset(), 0x80000000));

        int chatX = 4;
        int chatY = 40;
        int x = chatX + 2;
        int y = chatY + maxChatHeight + chatDiff - 2; //bottom border

        float scroll = chatDiff <= 0 ? 1f : this.scroll;
        y -= (int) (scroll * chatDiff);

        //chat background
        if (chatHeight > 0)
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, chatX, chatY + maxChatHeight - Math.min(chatHeight + 4, maxChatHeight), chatX + maxChatWidth, chatY + maxChatHeight, -UIHelper.getDepthOffset(), 0x80000000));

        //render messages
        UIHelper.pushStencil(matrices, chatX, chatY, maxChatWidth, maxChatHeight);

        for (Text message : messagesToRender) {
            int messageHeight = TextUtils.getHeight(message) + 2;

            if (y - messageHeight < chatY + maxChatHeight)
                message.render(VertexConsumer.MAIN, matrices, x, y, Alignment.BOTTOM_LEFT);

            y -= messageHeight;
            if (y < chatY) break;
        }

        UIHelper.popStencil();

        //render scrollbar
        if (chatDiff > 0) {
            int scrollbarHeight = (int) ((float) maxChatHeight / chatHeight * maxChatHeight);
            int scrollX = chatX + maxChatWidth - 3;
            int scrollY = chatY + (int) (scroll * (maxChatHeight - scrollbarHeight)) + 2;
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, scrollX, scrollY, scrollX + 1, scrollY + scrollbarHeight - 4, -UIHelper.getDepthOffset(), 0x80FFFFFF));
        }

        //render the super
        super.renderChildren(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        //super.renderBackground(matrices, delta);
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
                            MessageManager.addMessage(CommandParser.parseCommand(client.world.player, s.substring(1)), MessageCategory.SYSTEM, null);
                        //otherwise send as a chat message
                        else
                            MessageManager.addMessage(s, MessageCategory.CHAT, client.world.player);

                        //store the message
                        if (sentMessages.isEmpty() || !sentMessages.getLast().equals(s))
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
                //scroll up/down
                case GLFW_KEY_PAGE_UP,
                     GLFW_KEY_PAGE_DOWN -> {
                    int totalMessages = messagesToRender.size();
                    int shownMessages = totalMessages - scrollMessages;
                    float delta = (float) shownMessages / scrollMessages;
                    this.scroll = Maths.clamp(this.scroll + delta * (key == GLFW_KEY_PAGE_UP ? -1 : 1), 0f, 1f);
                }
                //scroll home
                case GLFW_KEY_HOME -> scroll = 0f;
                //scroll end
                case GLFW_KEY_END -> scroll = 1f;
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

    @Override
    public boolean scroll(double x, double y) {
        if (chatDiff <= 0) //no need to scroll
            return true;

        this.scroll = Maths.clamp(this.scroll + (float) -y * (1f / scrollMessages), 0f, 1f);
        return true;
    }

    protected void forceUpdate(List<Message> messages) {
        messagesToRender.clear();
        chatHeight = -2; //offset for the last padding

        for (int i = messages.size() - 1; i >= 0; i--) {
            //filter by category
            Message message = messages.get(i);
            if (selectedCategory != null && message.category() != selectedCategory)
                continue;

            //warp text
            Text text = message.text();
            List<Text> warped = TextUtils.warpToWidth(text, maxChatWidth - 4); //left and right borders
            for (int j = warped.size() - 1; j >= 0; j--) {
                Text warpedMessage = warped.get(j);
                int messageHeight = TextUtils.getHeight(warpedMessage);
                chatHeight += messageHeight + 2;
                messagesToRender.add(warpedMessage);
            }
        }

        chatDiff = chatHeight - maxChatHeight + 4; //top and bottom borders

        int totalMessages = messagesToRender.size();
        scrollMessages = totalMessages - (int) (((float) maxChatHeight / chatHeight) * totalMessages);
    }

    public void setFieldText(String text) {
        field.setText(text);
        field.setCursorToEnd();
    }
}
