package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.utils.Pair;
import cinnamon.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static cinnamon.commands.CommandParser.LOGGER;

public class Teleport implements Command {

    @Override
    public Text execute(Entity source, String[] args) {
        int i = 0;

        //parse position
        Pair<Vector3f, Integer> pos = CommandParser.parseCoordinate(source, args, i);
        if (pos.second() == 0)
            return Text.of("Failed to execute command, missing arguments");
        else if (pos.second() < 0)
            return Text.of("Failed to execute command, invalid argument: " + args[-pos.second() - 1 + i]);

        i += pos.second();
        Vector3f targetPos = pos.first();

        //parse rotation (optional)
        Pair<Vector2f, Integer> rot = CommandParser.parseRotation(source, args, i);
        if (rot.second() < 0)
            return Text.of("Failed to execute command, invalid argument: " + args[-rot.second() - 1 + i]);

        //apply position
        source.moveTo(targetPos);

        //apply rotation
        if (rot.second() == 0)
            return Text.of("Successfully teleported to " + targetPos);

        Vector2f targetRot = rot.first();
        source.rotateTo(targetRot);
        return Text.of("Successfully teleported to " + targetPos + " with rotation " + targetRot);
    }
}
