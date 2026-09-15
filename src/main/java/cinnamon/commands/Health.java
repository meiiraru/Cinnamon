package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Health implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        //parse target entity
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        if (!(target instanceof LivingEntity le))
            return Text.of("Target is not alive.").withStyle(ERROR_STYLE);

        //empty arg, query health
        if (args.isEmpty())
            return Text.of("Health: " + le.getHealth());

        //set the new health value
        String value = args.pop();
        try {
            int health = Integer.parseInt(value);
            le.setHealth(health);
            return Text.of("Health set to " + health);
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid argument: " + value).withStyle(ERROR_STYLE);
        }
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /health <target> [<value>]")
                .append("\n")
                .append("Gets or sets the health of the target");
    }
}
