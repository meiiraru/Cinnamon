package cinnamon.registry;

import cinnamon.commands.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.registry.Registry.LOGGER;

/**
 * A registry for commands, allowing for registration and retrieval of commands by their primary name or aliases
 */
public class CommandRegistry {

    private static final Map<String, CommandEntry> registry = new HashMap<>();

    //register the default engine commands
    static {
        register("help", new Help(), "h");
        register("teleport", new Teleport(), "tp");
        register("fill", new Fill());
        register("time", new Time(), "t");
        register("worldrule", new WorldRule(), "gamerule");
        register("kill", new Kill());
        register("health", new Health(), "hp");
        register("lookat", new LookAt(), "look");
        register("ride", new Ride(), "mount");
        register("explode", new Explode());
        register("spectate", new Spectate(), "spec", "sp");
        register("stopsound", new StopSound());
    }

    /**
     * Registers a command with its primary name and any optional aliases
     * @param name The primary name of the command
     * @param instance The command instance
     * @param aliases Any optional aliases for the command
     */
    public static void register(String name, Command instance, String... aliases) {
        String primaryName = name.toLowerCase();

        //add the command to the command registry
        CommandEntry entry = new CommandEntry(name, instance, aliases);

        //check if the command name already exists in the registry
        if (registry.containsKey(primaryName))
            LOGGER.warn("Overwriting duplicated command \"%s\" with instance \"%s\"!", primaryName, instance.getClass().getName());
        registry.put(primaryName, entry);
    }

    /**
     * Retrieves all registered command entries
     * @return A collection of all registered command entries
     */
    public static Collection<CommandEntry> getRegisteredCommands() {
        return Collections.unmodifiableCollection(registry.values());
    }

    /**
     * A record representing a command entry in the registry
     * @param name The primary name of the command
     * @param command The command instance
     * @param aliases Any optional aliases for the command
     */
    public record CommandEntry(String name, Command command, String... aliases) {}
}
