package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;

import java.util.Stack;

public interface Command {

    Text execute(Entity source, Stack<String> args);
}
