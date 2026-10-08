package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Interact implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        //parse target entity
        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        //check if its use or attack
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String arg = args.pop();
        boolean use = arg.equalsIgnoreCase("use");
        boolean attack = arg.equalsIgnoreCase("attack");

        if (!use && !attack)
            return Text.of("Invalid interaction type").withStyle(ERROR_STYLE);

        //execute the interaction as the source entity
        if (use) {
            target.useAction();
            return Text.of(target.getNameRepresentation() + " used");
        } else {
            target.attackAction();
            return Text.of(target.getNameRepresentation() + " attacked");
        }
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /run <target> <command>")
                .append("\n")
                .append("Executes a command as the specified target entity");
    }
}
