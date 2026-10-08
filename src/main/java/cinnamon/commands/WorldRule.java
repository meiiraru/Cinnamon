package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.WorldRules;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class WorldRule implements Command {

    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        if (args.isEmpty()) {
            Text text = Text.of("Available world rules: ");
            for (WorldRules.Rule<?> rule : WorldRules.getRegisteredRules())
                text.append(rule.getName().toLowerCase()).append(" ");
            return text;
        }

        //find the world rule
        String ruleStr = args.pop();
        WorldRules.Rule<?> rule = WorldRules.getRule(ruleStr);

        if (rule == null)
            return Text.of("Failed to execute command, invalid rule name: " + ruleStr).withStyle(ERROR_STYLE);

        //get value
        if (args.isEmpty())
            return Text.of(source.world().getRules().get(rule));

        //set value
        String valueStr = args.pop();
        try {
            source.world().getRules().parseAndSet(rule, valueStr);
            Object newValue = source.world().getRules().get(rule);
            return Text.of("Set world rule " + rule.getName() + " to " + newValue);
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid argument: " + valueStr).withStyle(ERROR_STYLE);
        }
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /worldrule [<rule>] [<value>]")
                .append("\n")
                .append("Gets or sets the value of a world rule");
    }
}
