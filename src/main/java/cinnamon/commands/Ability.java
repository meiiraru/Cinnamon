package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.Abilities;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Ability implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        //parse target
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Entity target = CommandParser.parseEntity(source, args.pop());
        if (target == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        if (!(target instanceof Player targetP))
            return Text.of("Target do not have abilities").withStyle(ERROR_STYLE);

        //parse ability
        if (args.isEmpty()) {
            //no ability provided - list all operation
            StringBuilder sb = new StringBuilder();
            for (Abilities.Ability ability : Abilities.Ability.values()) {
                boolean value = targetP.getAbilities().get(ability);
                sb.append(ability.name().toLowerCase()).append(": ").append(value).append("\n");
            }
            sb.setLength(sb.length() - 1); //remove last newline
            return Text.of("%s abilities:\n".formatted(target.getNameRepresentation())).append(sb.toString());
        }

        String abilityStr = args.pop();
        Abilities.Ability ability;

        try {
            ability = Abilities.Ability.valueOf(abilityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Text.of("Invalid ability: " + abilityStr).withStyle(ERROR_STYLE);
        }

        //parse value
        if (args.isEmpty()) {
            //no value provided - get operation
            boolean value = targetP.getAbilities().get(ability);
            return Text.of("%s ability %s is %s".formatted(target.getNameRepresentation(), ability.name().toLowerCase(), value));
        }

        String valueStr = args.pop();
        boolean value = Boolean.parseBoolean(valueStr);

        //apply ability value
        targetP.getAbilities().set(ability, value);
        return Text.of("Set %s ability %s to %s".formatted(target.getNameRepresentation(), ability.name().toLowerCase(), value));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /ability <target> [<ability>] [<value>]")
                .append("\n")
                .append("Sets or gets the ability of a target player");
    }
}
