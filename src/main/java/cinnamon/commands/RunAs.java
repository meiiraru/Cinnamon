package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class RunAs implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        //parse target entity
        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        source
                .setEntity(target)
                .setName(target.getNameRepresentation());

        //parse command to execute
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String command = String.join(" ", args.reversed());

        //execute the command as the target entity
        return Text.of("Executing command as " + target.getNameRepresentation())
                .append(" ")
                .append(CommandParser.runCommand(source, command));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /run <target> <command>")
                .append("\n")
                .append("Executes a command as the specified target entity");
    }
}
