package cinnamon.commands;

import cinnamon.logger.Logger;
import cinnamon.registry.CommandRegistry;
import cinnamon.registry.MaterialRegistry;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.Trie;
import cinnamon.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Stack;

public class CommandParser {

    public static final Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/command");
    public static final Style ERROR_STYLE = Style.EMPTY.color(Colors.RED);
    private static final Trie<Command> commandTrie = new Trie<>();

    static {
        for (CommandRegistry command : CommandRegistry.values()) {
            commandTrie.insert(command.name().toLowerCase(), command.command);
            for (String alias : command.aliases)
                commandTrie.insert(alias.toLowerCase(), command.command);
        }
    }

    public static Text parseCommand(Entity source, String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0)
            return Text.of("Unknown command").withStyle(ERROR_STYLE);

        String commandName = parts[0].toLowerCase();
        Stack<String> args = new Stack<>();
        for (int i = parts.length - 1; i > 0; i--)
            args.push(parts[i]);

        Command command = commandTrie.get(commandName);
        if (command != null)
            return command.execute(source, args);

        return Text.of("Unknown command: " + commandName).withStyle(ERROR_STYLE);
    }


    // -- helpers for parsing common arguments -- //


    static float parseRelativeFloat(String arg) throws NumberFormatException {
        return arg.length() > 1 ? Float.parseFloat(arg.substring(1)) : 0f;
    }

    static float parseRelative(String number, float relativeTo) throws NumberFormatException {
        if (number.startsWith("~")) {
            return parseRelativeFloat(number) + relativeTo;
        } else {
            return Float.parseFloat(number);
        }
    }

    static Vector3f parseCoordinate(Entity source, Stack<String> args) {
        if (args.size() < 3)
            return null;

        Vector3f result = new Vector3f();
        Vector3f relativePos = source.getPos();

        try {
            //check for directional coordinates
            if (args.getFirst().startsWith("^") && args.get(1).startsWith("^") && args.get(2).startsWith("^")) {
                for (int i = 0; i < 3; i++) {
                    result.setComponent(i, parseRelativeFloat(args.peek()));
                    args.pop();
                }

                result.rotate(Maths.rotToQuat(source.getRot()));
                result.add(relativePos);
            }

            //otherwise, parse as normal coordinates
            else {
                for (int i = 0; i < 3; i++) {
                    result.setComponent(i, parseRelative(args.peek(), relativePos.get(i)));
                    args.pop();
                }
            }

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    static Vector2f parseRotation(Entity source, Stack<String> args) {
        if (args.size() < 2)
            return null;

        Vector2f result = new Vector2f();
        Vector2f relativeRot = source.getRot();

        try {
            for (int i = 0; i < 2; i++) {
                result.setComponent(i, parseRelative(args.peek(), relativeRot.get(i)));
                args.pop();
            }

            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    static Integer parseMaterial(String arg) {
        if (arg.equalsIgnoreCase("air"))
            return -1;

        try {
            int id = Integer.parseInt(arg);
            return id < -1 || id >= MaterialRegistry.values().length ? null : id;
        } catch (Exception ignored) {}

        try {
            return MaterialRegistry.valueOf(arg.toUpperCase()).ordinal();
        } catch (Exception ignored) {
            return null;
        }
    }
}
