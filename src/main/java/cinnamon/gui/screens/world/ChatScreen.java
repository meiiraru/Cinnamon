package cinnamon.gui.screens.world;

import cinnamon.commands.CommandParser;
import cinnamon.gui.GUISkin;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.messages.Message;
import cinnamon.messages.MessageCategory;
import cinnamon.messages.MessageManager;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.*;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class ChatScreen extends Screen {

    public static final int MAX_SENT_HISTORY = 100;
    protected static final List<String> sentMessages = new CircularQueue<>(MAX_SENT_HISTORY);
    protected static String currentMessage = "";

    protected final WidgetList messageList;
    protected final TextField field;
    protected int sentIndex = -1;
    protected String backupText = "";
    protected int maxChatWidth, maxChatHeight, chatHeight;
    private Message lastMessage = null;
    protected MessageCategory selectedCategory = null;
    protected Text previewText = Text.empty();
    protected boolean showPreview;

    public ChatScreen() {
        //create text field
        field = new TextField(0, 0, 0, 0);
        field.setHintText(Text.translated("gui.chat.type_message").withStyle(MessageManager.DEFAULT_STYLE.color(Colors.LIGHT_GRAY)));
        field.setTextStyle(MessageManager.DEFAULT_STYLE);
        field.setTextOnly(true);
        field.setListener(str -> {
            //current message
            currentMessage = str;

            //preview message
            if (!str.startsWith("/")) {
                previewText = Text.empty()
                        .withStyle(MessageCategory.CHAT.getStyle())
                        .append(MarkdownParser.parseMarkdown(TextUtils.parseColorFormatting(Text.of(field.getText()))));

                showPreview = !str.equals(previewText.asString());
            } else {
                showPreview = false;
            }
        });

        if (!currentMessage.isEmpty()) {
            field.setText(currentMessage);
            field.selectAll();
        }

        //create message list
        messageList = new WidgetList(0, 0, 0, 0, 2);
        messageList.setScrollPadding(0);
        messageList.setShowScrollbar(false);
        messageList.setIgnoreScrollbarOffset(true);
        messageList.setAlignment(Alignment.BOTTOM_LEFT);
        messageList.forceChildAlignment(true);
    }

    @Override
    public void init() {
        super.init();

        //update field dimensions
        int fh = (int) (GUISkin.getCurrentSkin().getFont().lineHeight + 2);
        field.setPos(0, height - fh - 24);
        field.setDimensions(width, fh);

        //add and focus field
        addWidget(field);
        this.focusWidget(field);

        maxChatWidth = 350;
        maxChatHeight = field.getY() - 4 - 40;

        //update messageList
        messageList.setPos(4 + 2, field.getY() - 4 - 2);
        messageList.setDimensions(maxChatWidth, 0);
        messageList.scrollToBottom();
        addWidget(messageList);

        lastMessage = null;
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

        //chat background
        if (chatHeight > 0)
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(
                    matrices,
                    messageList.getAlignedX() - 2, messageList.getAlignedY() - 2,
                    messageList.getAlignedX() + messageList.getWidth() + 2, messageList.getAlignedY() + messageList.getHeight() + 2,
                    -UIHelper.getDepthOffset(), 0x80000000
            ));

        //render scrollbar
        if (messageList.isScrollbarNeeded()) {
            int scrollbarHeight = (int) ((float) maxChatHeight / chatHeight * maxChatHeight);
            int scrollX = chatX + maxChatWidth;
            int scrollY = chatY + (int) (messageList.getScrollbar().getPercentage() * (maxChatHeight - scrollbarHeight));
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, scrollX + 1, scrollY, scrollX + 2, scrollY + scrollbarHeight - 4, -UIHelper.getDepthOffset(), 0x80FFFFFF));
        }

        //render preview message
        if (showPreview) {
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, field.getX(), field.getY() + field.getHeight() + 2, field.getX() + field.getWidth(), field.getY() + field.getHeight() + 2 + field.getHeight(), -UIHelper.getDepthOffset(), 0x80000000));
            previewText.render(VertexConsumer.MAIN, matrices, field.getX() + 2, field.getY() + field.getHeight() + 2 + field.getHeight() - 2, Alignment.BOTTOM_LEFT);
        }

        //render the super
        super.renderChildren(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
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
                            MessageManager.addMessage(CommandParser.runCommand(client.world.player, s.substring(1)), MessageCategory.SYSTEM, null);
                        //otherwise send as a chat message
                        else
                            MessageManager.addMessage(s, MessageCategory.CHAT, client.world.player);

                        //store the message
                        if (sentMessages.isEmpty() || !sentMessages.getLast().equals(s))
                            sentMessages.add(s);
                    }

                    return true;
                }
                //suggest autocomplete for the current message
                case GLFW_KEY_TAB -> {
                    //nothing yet...
                    return true;
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
                case GLFW_KEY_PAGE_UP -> {
                    messageList.getScrollbar().forceScroll(0f, 1f);
                    return true;
                }
                case GLFW_KEY_PAGE_DOWN -> {
                    messageList.getScrollbar().forceScroll(0f, -1f);
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

    @Override
    public boolean scroll(double x, double y) {
        return messageList.getScrollbar().forceScroll(x, y) != null;
    }

    protected void forceUpdate(List<Message> messages) {
        messageList.clear();
        int spacing = messageList.getSpacing();
        chatHeight = -spacing; //offset for the last padding

        for (Message message : messages) {
            //filter by category
            if (selectedCategory != null && message.category() != selectedCategory)
                continue;

            //warp text
            int width = maxChatWidth - 4; //left and right borders
            Text text = message.text();

            Label l = new Label(0, 0, text);
            l.setMaxWidth(width);
            messageList.addWidget(l);

            chatHeight += l.getHeight() + spacing; //padding
        }

        messageList.setHeight(Math.min(chatHeight, maxChatHeight));
        messageList.forceUpdate();
    }

    public void setFieldText(String text) {
        field.setText(text);
        field.setCursorToEnd();
    }
}
