package cinnamon.commands;

import cinnamon.utils.Pair;
import cinnamon.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static cinnamon.commands.CommandParser.LOGGER;

public class Teleport implements Command {

    @Override
    public void execute(Entity source, String[] args) {
        try {
            int i = 0;

            Pair<Vector3f, Integer> pos = CommandParser.parseCoordinate(source, args, i);
            if (pos.second() == 0)
                return;

            i += pos.second();

            Vector3f targetPos = pos.first();
            source.moveTo(targetPos);

            Pair<Vector2f, Integer> rot = CommandParser.parseRotation(source, args, i);
            if (rot.second() == 0)
                return;

            Vector2f targetRot = rot.first();
            source.rotateTo(targetRot);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }
}
