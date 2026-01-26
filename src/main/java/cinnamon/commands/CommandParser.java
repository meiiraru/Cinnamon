package cinnamon.commands;

import cinnamon.logger.Logger;
import cinnamon.registry.CommandRegistry;
import cinnamon.utils.Maths;
import cinnamon.utils.Pair;
import cinnamon.utils.Trie;
import cinnamon.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class CommandParser {

    public static final Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/command");
    private static final Trie<Command> commandTrie = new Trie<>();

    static {
        for (CommandRegistry command : CommandRegistry.values()) {
            commandTrie.insert(command.name().toLowerCase(), command.command);
            for (String alias : command.aliases)
                commandTrie.insert(alias.toLowerCase(), command.command);
        }
    }

    public static void parseCommand(Entity source, String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String commandName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Command command = commandTrie.get(commandName);
        if (command != null)
            command.execute(source, args);
    }


    // -- helpers for parsing common arguments -- //


    static float parseFloat(String arg) throws NumberFormatException {
        return arg.length() > 1 ? Float.parseFloat(arg.substring(1)) : 0f;
    }

    static void parseVec2(String[] args, int i, Vector2f out) throws NumberFormatException {
        out.x = parseFloat(args[i]);
        out.y = parseFloat(args[i + 1]);
    }

    static void parseVec3(String[] args, int i, Vector3f out) throws NumberFormatException {
        out.x = parseFloat(args[i]);
        out.y = parseFloat(args[i + 1]);
        out.z = parseFloat(args[i + 2]);
    }

    static Pair<Vector3f, Integer> parseCoordinate(Entity source, String[] args, int i) throws NumberFormatException {
        if (i + 2 >= args.length)
            return Pair.of(null, 0);

        Vector3f target = new Vector3f();

        //relative directional position
        if (args[i].startsWith("^") && args[i + 1].startsWith("^") && args[i + 2].startsWith("^")) {
            parseVec3(args, i, target);

            Quaternionf relativeRot = Maths.rotToQuat(source.getRot());
            target.rotate(relativeRot);
            target.add(source.getPos());

            return Pair.of(target, 3);
        }

        //relative position
        if (args[i].startsWith("~") && args[i + 1].startsWith("~") && args[i + 2].startsWith("~")) {
            parseVec3(args, i, target);
            target.add(source.getPos());
            return Pair.of(target, 3);
        }

        //absolute position
        target.x = Float.parseFloat(args[i]);
        target.y = Float.parseFloat(args[i + 1]);
        target.z = Float.parseFloat(args[i + 2]);
        return Pair.of(target, 3);
    }

    static Pair<Vector2f, Integer> parseRotation(Entity source, String[] args, int i) throws NumberFormatException {
        if (i + 1 >= args.length)
            return Pair.of(null, 0);

        Vector2f targetRot = new Vector2f();

        //relative rotation
        if (args[i].startsWith("~") && args[i + 1].startsWith("~")) {
            parseVec2(args, i, targetRot);
            targetRot.add(source.getRot());
            return Pair.of(targetRot, 2);
        }

        //absolute rotation
        targetRot.x = Float.parseFloat(args[i]);
        targetRot.y = Float.parseFloat(args[i + 1]);
        return Pair.of(targetRot, 2);
    }
}
