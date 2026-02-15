package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;

public interface Command {

    Text execute(Entity source, String[] args);
}
