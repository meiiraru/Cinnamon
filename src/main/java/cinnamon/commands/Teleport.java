package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Teleport implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        //parse position
        if (args.size() < 3)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector3f pos = CommandParser.parseCoordinate(source, args);
        if (pos == null)
            return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

        //parse rotation (optional)
        if (!args.isEmpty()) {
            if (args.size() < 2)
                return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

            Vector2f rot = CommandParser.parseRotation(source, args);
            if (rot == null)
                return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

            //apply position
            source.moveTo(pos);

            //apply rotation
            source.rotateTo(rot);
            return Text.of("Teleported to %.3f %.3f %.3f rotated %.3f %.3f".formatted(pos.x, pos.y, pos.z, rot.x, rot.y));
        }

        //apply only position
        source.moveTo(pos);
        return Text.of("Teleported to %.3f %.3f %.3f".formatted(pos.x, pos.y, pos.z));
    }
}
