package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.WorldRules;
import cinnamon.world.entity.Entity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class WorldRule implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String ruleStr = args.pop();
        WorldRules.Rule rule;

        try {
            rule = WorldRules.Rule.valueOf(ruleStr.toUpperCase());
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid argument: " + ruleStr).withStyle(ERROR_STYLE);
        }

        if (args.isEmpty())
            return Text.of(source.getWorld().getRules().get(rule));

        String valueStr = args.pop();
        try {
            Object value;

            switch (rule.type) {
                case INT   -> value = Integer.parseInt(valueStr);
                case FLOAT -> value = Float.parseFloat(valueStr);
                case BOOL  -> value = CommandParser.parseBoolean(valueStr);
                default    -> value = valueStr;
            }

            source.getWorld().getRules().set(rule, value);
            return Text.of("Set world rule " + ruleStr + " to " + value);
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid argument: " + valueStr).withStyle(ERROR_STYLE);
        }
    }
}
