package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Ride implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        //try to stop riding
        if (args.isEmpty()) {
            if (!source.isRiding())
                return Text.of("Nothing to ride");

            source.stopRiding();
            return Text.of("Stopped riding");
        }

        Entity target = CommandParser.parseEntity(source, args.pop());

        if (target == null) {
            return Text.of("Target not found").withStyle(ERROR_STYLE);
        } else {
            target.addRider(source);
            return Text.of("Now riding ").append(target.getName() == null ? target.getUUID() : target.getName());
        }
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /ride [<target>]")
                .append("\n")
                .append("Makes the source entity ride/stop riding the specified target entity");
    }
}
