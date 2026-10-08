package cinnamon.commands;

import cinnamon.messages.MessageCategory;
import cinnamon.messages.MessageManager;
import cinnamon.text.Text;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Say implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String message = String.join(" ", args.reversed());
        MessageManager.addMessage(message, MessageCategory.CHAT, source.entity());

        return Text.of("[%s] said \"%s\"".formatted(source.name(), message));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /say <text>")
                .append("\n")
                .append("Sends a message to the chat with the specified text");
    }
}
