package cinnamon.registry;

import cinnamon.commands.Command;
import cinnamon.commands.Teleport;

public enum CommandRegistry {

    TP(new Teleport(), "teleport", "banana");

    public final Command command;
    public final String[] aliases;

    CommandRegistry(Command instance, String... aliases) {
        this.command = instance;
        this.aliases = aliases;
    }
}
