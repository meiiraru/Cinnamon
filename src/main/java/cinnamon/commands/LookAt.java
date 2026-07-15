package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import org.joml.Vector3f;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class LookAt implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        //parse position
        if (args.size() < 3)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector3f pos = CommandParser.parseCoordinate(source, args);
        if (pos == null)
            return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

        //apply rotation to look at the position
        source.lookAt(pos);
        return Text.of("Looking at %.3f %.3f %.3f".formatted(pos.x, pos.y, pos.z));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /lookat <x> <y> <z>")
                .append("\n")
                .append("Rotates the player to look at the specified world coordinates");
    }
}
