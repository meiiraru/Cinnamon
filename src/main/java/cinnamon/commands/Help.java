package cinnamon.commands;

import cinnamon.registry.CommandRegistry;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.world.entity.Entity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Help implements Command {

    public static final Style HELP_STYLE = Style.EMPTY.italic(false).color(Colors.WHITE);

    @Override
    public Text execute(Entity source, Stack<String> args) {
        if (args.isEmpty())
            return getHelpCommand().append("\n").append(getCommandList()).withStyle(HELP_STYLE);

        //parse command
        String commandName = args.pop().toLowerCase();
        Command command = CommandParser.commandTrie.get(commandName);

        if (command == null)
            return Text.of("Unknown command: " + commandName).withStyle(ERROR_STYLE);

        return command.getHelpCommand().withStyle(HELP_STYLE);
    }

    private Text getCommandList() {
        StringBuilder str = new StringBuilder();

        //add commands and its aliases
        for (CommandRegistry command : CommandRegistry.values()) {
            //add the command itself
            str.append("/");
            str.append(command.name().toLowerCase());

            //if we have aliases, add them
            String[] aliases = command.aliases;
            if (aliases.length > 0) {
                str.append(", ");
                str.append(String.join(", ", aliases));
            }

            str.append("\n");
        }

        //remove last newline
        if (!str.isEmpty())
            str.deleteCharAt(str.length() - 1);

        //return the string as text
        return Text.of(str.toString());
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Use /help <command> for more info on a specific command");
    }
}
