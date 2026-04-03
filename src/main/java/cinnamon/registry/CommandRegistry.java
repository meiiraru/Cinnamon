package cinnamon.registry;

import cinnamon.commands.*;

public enum CommandRegistry {

    TP(new Teleport(), "teleport"),
    FILL(new Fill()),
    TIME(new Time(), "t"),
    WORLDRULE(new WorldRule(), "gamerule"),
    KILL(new Kill()),
    HEALTH(new Health(), "hp");

    public final Command command;
    public final String[] aliases;

    CommandRegistry(Command instance, String... aliases) {
        this.command = instance;
        this.aliases = aliases;
    }
}
