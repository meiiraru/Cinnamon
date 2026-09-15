package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import org.joml.Vector3f;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class LookAt implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        Vector3f pos;

        //get position
        if (args.size() < 3) {
            //parse target entity
            Entity target = CommandParser.parseEntity(source, args.pop());
            if (target == null)
                return Text.of("Target not found").withStyle(ERROR_STYLE);

            //check if we should look at feet or eyes
            if (!args.isEmpty()) {
                String arg = args.pop();
                if (arg.equalsIgnoreCase("feet"))
                    pos = target.getTransform().getPos();
                else if (arg.equalsIgnoreCase("eyes"))
                    pos = target.getEyePos();
                else
                    return Text.of("Invalid argument: " + arg).withStyle(ERROR_STYLE);
            } else {
                //default to feet if no argument is provided
                pos = target.getTransform().getPos();
            }
        } else {
            //parse coordinates
            pos = CommandParser.parseCoordinate(source, args);
            if (pos == null)
                return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);
        }

        //apply rotation to look at the position
        source.lookAt(pos);
        return Text.of("Looking at %.3f %.3f %.3f".formatted(pos.x, pos.y, pos.z));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /lookat (<target> [feet|eyes]|<x> <y> <z>)")
                .append("\n")
                .append("Rotates the caller to look at the specified entity feet or eyes, or a specified world coordinates");
    }
}
