package cinnamon.messages;

import cinnamon.Client;
import cinnamon.logger.Logger;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.CircularQueue;
import cinnamon.utils.TextUtils;
import cinnamon.world.entity.Entity;

import java.util.List;

public class MessageManager {

    public static final Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/message");
    public static final Style DEFAULT_STYLE = Style.EMPTY.shadow(true);
    public static final int
            MAX_MESSAGES = 100,
            RECENT_MESSAGES = 8;

    private static final List<Message> messages = new CircularQueue<>(MAX_MESSAGES);

    public static void addMessage(String msg, MessageCategory category, Entity source) {
        addMessage(Text.of(msg), category, source);
    }

    public static void addMessage(Text msg, MessageCategory category, Entity source) {
        if (source == null)
            LOGGER.info("[%s] %s", category.name(), msg.asString());
        else
            LOGGER.info("[%s] [%s] %s", category.name(), source.getName(), msg.asString());

        Text text = Text.empty().withStyle(category.getStyle()).append(TextUtils.parseColorFormatting(msg));
        messages.add(new Message(Client.getInstance().ticks, text, category, source));
    }

    public static List<Message> getMessages() {
        return messages;
    }

    public static void clearMessages() {
        messages.clear();
    }
}
