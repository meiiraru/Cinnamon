package cinnamon.commands;

import cinnamon.math.collision.Sphere;
import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import org.joml.Vector3f;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Explode implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        Vector3f pos = source.getTransform().getPos();
        float radius = 2f;
        float strength = 1f;

        //self explode
        if (args.isEmpty()) {
            source.getWorld().explode(new Sphere(pos, radius), strength, null, false);
            return Text.of("Exploded");
        }

        //first try pos
        if (args.size() >= 3) {
            pos = CommandParser.parseCoordinate(source, args);
            if (pos == null)
                return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);
        }

        //then try radius
        if (!args.isEmpty()) {
            String val = args.pop();
            try {
                radius = Float.parseFloat(val);
            } catch (NumberFormatException e) {
                return Text.of("Failed to execute command, invalid argument: " + val).withStyle(ERROR_STYLE);
            }
        }

        //and finally try strength
        if (!args.isEmpty()) {
            String val = args.pop();
            try {
                strength = Float.parseFloat(val);
            } catch (NumberFormatException e) {
                return Text.of("Failed to execute command, invalid argument: " + val).withStyle(ERROR_STYLE);
            }
        }

        //explode!
        source.getWorld().explode(new Sphere(pos, radius), strength, null, false);
        return Text.of("Exploded");
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /explode [<x y z>] [<radius>] [<power>]")
                .append("\n")
                .append("Creates an explosion at the specified coordinates (optional) with the specified radius and power (optional)");
    }
}
