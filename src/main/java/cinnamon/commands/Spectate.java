package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.world.WorldClient;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Spectate implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        Entity target = args.isEmpty() ? source : CommandParser.parseEntity(source, args.pop());

        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        ((WorldClient) source.getWorld()).cameraEntity = target;

        String name = target.getName();
        return Text.of("Now spectating ").append(name == null || name.isBlank() ? target.getUUID() : name);
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /spectate [<target>]")
                .append("\n")
                .append("Sets the camera to follow the specified target");
    }
}
