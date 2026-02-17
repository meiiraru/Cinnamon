package cinnamon.commands;

import cinnamon.logger.Logger;
import cinnamon.registry.CommandRegistry;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.Pair;
import cinnamon.utils.Trie;
import cinnamon.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

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
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

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

    static Pair<Vector3f, Integer> parseCoordinate(Entity source, String[] args, int i) {
        if (i + 2 >= args.length)
            return Pair.of(null, 0);

        int index = 0;
        Vector3f result = new Vector3f();
        Vector3f relativePos = source.getPos();

        try {
            //check for directional coordinates
            if (args[i].startsWith("^") && args[i + 1].startsWith("^") && args[i + 2].startsWith("^")) {
                result.set(
                        parseRelativeFloat(args[i + index++]),
                        parseRelativeFloat(args[i + index++]),
                        parseRelativeFloat(args[i + index++])
                );
                result.rotate(Maths.rotToQuat(source.getRot()));
                result.add(relativePos);
            }

            //otherwise, parse as normal coordinates
            else {
                for (; index < 3; index++)
                    result.setComponent(index, parseRelative(args[i + index], relativePos.get(index)));
            }

            return Pair.of(result, index);
        } catch (Exception e) {
            LOGGER.error("Failed to parse coordinate", e);
            return Pair.of(null, -++index);
        }
    }

    static Pair<Vector2f, Integer> parseRotation(Entity source, String[] args, int i) {
        if (i + 1 >= args.length)
            return Pair.of(null, 0);

        int index = 0;
        Vector2f result = new Vector2f();
        Vector2f relativeRot = source.getRot();

        try {
            for (; index < 2; index++)
                result.setComponent(index, parseRelative(args[i + index], relativeRot.get(index)));

            return Pair.of(result, index);
        } catch (Exception e) {
            LOGGER.error("Failed to parse rotation", e);
            return Pair.of(null, -++index);
        }
    }
}
