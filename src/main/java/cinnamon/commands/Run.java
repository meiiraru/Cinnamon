package cinnamon.commands;

import cinnamon.math.Maths;
import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Run implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        while (!args.isEmpty()) {
            String subcommand = args.pop();
            Text result = switch (subcommand) {
                case "aligned" -> aligned(source, args);
                case "anchored" -> anchored(source, args);
                case "as" -> as(source, args);
                case "at" -> at(source, args);
                case "facing" -> facing(source, args);
                case "on" -> on(source, args);
                case "positioned" -> positioned(source, args);
                case "rotated" -> rotated(source, args);
                case "run" -> run(source, args);
                default -> Text.of("Failed to execute command, invalid argument: " + subcommand).withStyle(ERROR_STYLE);
            };

            if (result != null)
                return result;
        }

        return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);
    }

    private Text aligned(CommandSource source, Stack<String> args) {
        String str = args.pop().toLowerCase();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case 'x' -> source.setPosition(new Vector3f(Math.floor(source.position().x), source.position().y, source.position().z));
                case 'y' -> source.setPosition(new Vector3f(source.position().x, Math.floor(source.position().y), source.position().z));
                case 'z' -> source.setPosition(new Vector3f(source.position().x, source.position().y, Math.floor(source.position().z)));
                default -> {
                    return Text.of("Failed to execute command, invalid argument: " + c).withStyle(ERROR_STYLE);
                }
            }
        }
        return null;
    }

    private Text anchored(CommandSource source, Stack<String> args) {
        //parse string
        String str = args.pop().toLowerCase();
        switch (str) {
            case "feet" -> {
                if (source.entity() != null)
                    source.setPosition(source.entity().getTransform().getPos());
            }
            case "eyes" -> {
                if (source.entity() != null)
                    source.setPosition(source.entity().getEyePos());
            }
            default -> {
                return Text.of("Failed to execute command, invalid argument: " + str).withStyle(ERROR_STYLE);
            }
        }
        return null;
    }

    private Text as(CommandSource source, Stack<String> args) {
        //parse target entity
        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        source
                .setEntity(target)
                .setName(target.getNameRepresentation());
        return null;
    }

    private Text at(CommandSource source, Stack<String> args) {
        //parse target entity
        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        source.setPosition(target.getTransform().getPos());
        source.setRotation(target.getTransform().getRot());
        source.setDirection(target.getLookDir());
        source.setWorld(target.getWorld());
        return null;
    }

    private Text facing(CommandSource source, Stack<String> args) {
        Vector3f pos;

        //try to parse as entity
        String arg = args.peek();
        Entity target = CommandParser.parseEntity(source, arg);

        if (target != null) {
            args.pop();
            pos = target.getTransform().getPos();

            //optional eyes or feet argument
            if (!args.isEmpty()) {
                String eyes = args.peek().toLowerCase();
                switch (eyes) {
                    case "feet" -> args.pop();
                    case "eyes" -> {
                        pos = target.getEyePos();
                        args.pop();
                    }
                }
            }
        }
        //try to parse as coordinates
        else {
            if (args.size() < 3)
                return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

            pos = CommandParser.parseCoordinate(source, args);
            if (pos == null)
                return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);
        }

        //calculate direction
        Vector3f direction = new Vector3f(source.position()).sub(pos).normalize();
        source
                .setDirection(direction)
                .setRotation(Maths.dirToQuat(direction));
        return null;
    }

    private Text on(CommandSource source, Stack<String> args) {
        String arg = args.pop().toLowerCase();
        Entity target = null;

        switch (arg) {
            case "self" -> {
                if (source.entity() != null)
                    target = source.entity();
            }
            case "controller" -> {
                if (source.entity() != null)
                    target = source.entity().getControllingEntity();
            }
            case "ride" -> {
                if (source.entity() != null)
                    target = source.entity().getRidingEntity();
            }
            default -> {
                return Text.of("Failed to execute command, invalid argument: " + arg).withStyle(ERROR_STYLE);
            }
        }

        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        source.setEntity(target);
        source.setName(target.getNameRepresentation());

        return null;
    }

    private Text positioned(CommandSource source, Stack<String> args) {
        if (args.size() < 3)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector3f pos = CommandParser.parseCoordinate(source, args);
        if (pos == null)
            return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

        source.setPosition(pos);
        return null;
    }

    private Text rotated(CommandSource source, Stack<String> args) {
        //"as" copy target rotation
        String arg = args.peek().toLowerCase();
        if (arg.equals("as")) {
            args.pop();
            Entity target = CommandParser.parseEntity(source, args.pop());
            if (target == null)
                return Text.of("Target not found").withStyle(ERROR_STYLE);

            source
                    .setRotation(target.getTransform().getRot())
                    .setDirection(target.getLookDir());
            return null;
        }

        //otherwise parse rotation
        if (args.size() < 2)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector2f rot = CommandParser.parseRotation(source, args);
        if (rot == null)
            return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

        source
                .setRotation(Maths.rotToQuat(rot))
                .setDirection(Maths.quatToDir(source.rotation()));
        return null;
    }

    private Text run(CommandSource source, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String command = String.join(" ", args.reversed());

        //execute the command as the target entity
        return Text.of(source.name())
                .append(" \u2192 ")
                .append(CommandParser.runCommand(source, command));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /run <[aligned|anchored|as|at|facing|on|positioned|rotated]> run <command>")
                .append("\n")
                .append("Executes a command aligned to a position, " +
                        "anchored to the executioner feet/eyes, " +
                        "as a target entity, " +
                        "at a target entity, " +
                        "facing a target entity or position, " +
                        "on a related entity, " +
                        "positioned at a target position, " +
                        "or rotated to a target rotation"
                );
    }
}
