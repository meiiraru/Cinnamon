package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Effect implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        //parse target entity
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        if (!(target instanceof LivingEntity le))
            return Text.of("Target is immune to effects").withStyle(ERROR_STYLE);

        //parse mode
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        boolean clear = false;
        String mode = args.pop();
        if (mode.equalsIgnoreCase("clear")) {
            clear = true;
        } else if (!mode.equalsIgnoreCase("give")) {
            return Text.of("Failed to execute command, invalid argument: " + mode).withStyle(ERROR_STYLE);
        }

        //parse effect
        if (args.isEmpty()) {
            //clear all
            if (clear) {
                le.removeAllEffects();
                return Text.of("Cleared all effects from " + target.getNameRepresentation());
            }

            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);
        }

        String effectName = args.pop();
        cinnamon.world.effects.Effect.Type effectType;
        try {
            effectType = cinnamon.world.effects.Effect.Type.valueOf(effectName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Text.of("Invalid effect type: " + effectName).withStyle(ERROR_STYLE);
        }

        //clear specific effect
        if (clear) {
            le.removeEffect(effectType);
            return Text.of("Cleared effect \"%s\" from %s".formatted(effectType.name(), target.getNameRepresentation()));
        }

        //parse time
        int time = 15;
        if (!args.isEmpty()) {
            String val = args.pop();
            try {
                time = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return Text.of("Failed to execute command, invalid argument: " + val).withStyle(ERROR_STYLE);
            }
        }

        //parse strength
        int strength = 1;
        if (!args.isEmpty()) {
            String val = args.pop();
            try {
                strength = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return Text.of("Failed to execute command, invalid argument: " + val).withStyle(ERROR_STYLE);
            }
        }

        //apply effect
        le.giveEffect(effectType.create(time * 20, strength));
        return Text.of("Applied effect \"%s\" to %s [time=%ds] [str=%d]".formatted(effectType.name(), target.getNameRepresentation(), time, strength));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /effect <target> <give|clear> <effect> [<time>] [<strength>]")
                .append("\n")
                .append("Applies a status effect to the target entity");
    }
}
