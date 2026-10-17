package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import org.joml.Vector3f;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Impulse implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        //parse target
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        PhysEntity physTarget = target instanceof PhysEntity ? (PhysEntity) target : null;
        if (physTarget == null)
            return Text.of("Target is not pushable").withStyle(ERROR_STYLE);

        //parse dir
        Vector3f dir = source.direction();
        if (!args.isEmpty()) {
            dir = CommandParser.parseDirection(args);
            if (dir == null)
                return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);
        }

        //parse strength
        float strength = 1f;
        if (!args.isEmpty()) {
            try {
                strength = Float.parseFloat(args.pop());
            } catch (NumberFormatException e) {
                return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);
            }
        }

        physTarget.knockback(dir, strength);
        return Text.of("Pushed %s [dir=%.2f, %.2f, %.2f] [str=%.2f]".formatted(target.getNameRepresentation(), dir.x, dir.y, dir.z, strength));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /impulse <target> [<direction>] [<strength>]")
                .append("\n")
                .append("Pushes the target entity in the specified direction with the specified strength");
    }
}
