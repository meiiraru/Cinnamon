package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Health implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        if (!(source instanceof LivingEntity le))
            return Text.of("Source is not alive.").withStyle(CommandParser.ERROR_STYLE);

        if (args.isEmpty())
            return Text.of("Health: " + le.getHealth());

        String value = args.pop();
        try {
            int health = Integer.parseInt(value);
            le.setHealth(health);
            return Text.of("Health set to " + health);
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid argument: " + value).withStyle(ERROR_STYLE);
        }
    }
}
