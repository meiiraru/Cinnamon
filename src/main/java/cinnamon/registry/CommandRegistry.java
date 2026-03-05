package cinnamon.registry;

import cinnamon.commands.Command;
import cinnamon.commands.Fill;
import cinnamon.commands.Teleport;
import cinnamon.commands.Time;

public enum CommandRegistry {

    TP(new Teleport(), "teleport"),
    FILL(new Fill()),
    TIME(new Time(), "t");

    public final Command command;
    public final String[] aliases;

    CommandRegistry(Command instance, String... aliases) {
        this.command = instance;
        this.aliases = aliases;
    }
}
