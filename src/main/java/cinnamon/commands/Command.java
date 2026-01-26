package cinnamon.commands;

import cinnamon.world.entity.Entity;

public interface Command {

    void execute(Entity source, String[] args);
}
