package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Teleport implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        //parse target
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        //parse position
        Vector3f pos;

        if (args.isEmpty()) {
            pos = source.position();
            target.moveTo(pos);
            target.rotateTo(source.rotation());
            return Text.of("Teleported %s to %.3f %.3f %.3f".formatted(target.getNameRepresentation(), pos.x, pos.y, pos.z));
        }

        //try to parse as target
        Entity targetPos = CommandParser.parseEntity(source, args.peek());
        if (targetPos != null) {
            pos = targetPos.getTransform().getPos();
            args.pop();
        }
        //then try to parse as coordinates
        else {
            if (args.size() < 3)
                return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

            pos = CommandParser.parseCoordinate(source, args);
            if (pos == null)
                return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);
        }


        if (args.isEmpty()) {
            //apply only position
            target.moveTo(pos);
            return Text.of("Teleported %s to %.3f %.3f %.3f".formatted(target.getNameRepresentation(), pos.x, pos.y, pos.z));
        }

        //parse rotation (optional)

        //try to parse as facing direction
        String facing = args.peek().toLowerCase();
        if (facing.equals("facing")) {
            args.pop();

            if (args.isEmpty())
                return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

            Vector3f look;

            //parse as looking entity
            if (args.size() < 3) {
                Entity lookTarget = CommandParser.parseEntity(source, args.pop());
                if (lookTarget == null)
                    return Text.of("Target not found").withStyle(ERROR_STYLE);

                //check if we should look at feet or eyes
                if (!args.isEmpty()) {
                    String arg = args.pop().toLowerCase();
                    switch (arg) {
                        case "feet" -> look = lookTarget.getTransform().getPos();
                        case "eyes" -> look = lookTarget.getEyePos();
                        default -> {
                            return Text.of("Invalid argument: " + arg).withStyle(ERROR_STYLE);
                        }
                    }
                } else {
                    //default to feet if no argument is provided
                    look = lookTarget.getTransform().getPos();
                }
            }
            //parse as coordinates
            else {
                look = CommandParser.parseCoordinate(source, args);
                if (look == null)
                    return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);
            }

            //apply position
            target.moveTo(pos);

            //apply rotation
            target.lookAt(look);
            return Text.of("Teleported %s to %.3f %.3f %.3f looking at %.3f %.3f %.3f".formatted(target.getNameRepresentation(), pos.x, pos.y, pos.z, look.x, look.y, look.z));
        }

        //parse as rotation
        if (args.size() < 2)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector2f rot = CommandParser.parseRotation(source, args);
        if (rot == null)
            return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

        //apply position
        target.moveTo(pos);

        //apply rotation
        target.rotateTo(rot.x, rot.y, 0);
        return Text.of("Teleported %s to %.3f %.3f %.3f rotated %.3f %.3f".formatted(target.getNameRepresentation(), pos.x, pos.y, pos.z, rot.x, rot.y));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /teleport <target> [<target>|<x> <y> <z>] [<pitch> <yaw>|facing <target entity <feet|eyes>| <position>>]")
                .append("\n")
                .append("Teleports the target entity to the specified coordinates with optional rotation or looking direction");
    }
}
