package cinnamon.commands;

import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundManager;
import cinnamon.text.Text;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class StopSound implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        //parse category
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String categoryStr = args.pop();

        //stop all sounds
        if (categoryStr.equalsIgnoreCase("all")) {
            SoundManager.stopAll();
            return Text.of("Stopped all sounds");
        }

        //stop by the specified category
        try {
            SoundCategory category = SoundCategory.valueOf(categoryStr.toUpperCase());
            SoundManager.stopAll(cat -> cat == category);
            return Text.of("Stopped all sounds in category: " + categoryStr);
        } catch (IllegalArgumentException e) {
            return Text.of("Invalid sound category: " + categoryStr).withStyle(ERROR_STYLE);
        }
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /stopsound <category|all>")
                .append("\n")
                .append("Stops all sounds or all sounds in the specified category");
    }
}
