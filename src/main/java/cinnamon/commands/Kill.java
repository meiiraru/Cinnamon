package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Kill implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        Entity target = args.isEmpty() ? source : CommandParser.parseEntity(source, args.pop());

        if (target == null)
            return Text.of("Target not found.").withStyle(ERROR_STYLE);

        if (target instanceof LivingEntity le && !le.isDead()) {
            le.kill();
            return Text.of("Entity killed.");
        } else {
            target.remove();
            return Text.of("Entity removed.");
        }
    }
}
