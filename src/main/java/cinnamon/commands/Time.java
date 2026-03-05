package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.world.World;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Time implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        World world = source.getWorld();

        if (args.isEmpty())
            return Text.of(world.getTime());

        String subcommand = args.pop();
        return switch (subcommand) {
            case "query" -> timeQuery(world, args);
            case "set" -> timeSet(world, args);
            case "add" -> timeAdd(world, args);
            default -> Text.of("Failed to execute command, invalid argument: " + subcommand).withStyle(ERROR_STYLE);
        };
    }

    private Text timeQuery(World world, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String subcommand = args.pop();
        return switch (subcommand) {
            case "day" -> Text.of(world.getDay());
            case "time" -> Text.of(world.getTime());
            case "clock" -> Text.of(world.getTimeOfTheDay());
            default -> Text.of("Failed to execute command, invalid argument: " + subcommand).withStyle(ERROR_STYLE);
        };
    }

    private Text timeSet(World world, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String value = args.pop();

        //attempt to parse the time as a long
        try {
            long time = Long.parseLong(value);
            world.setTime(time);
            return Text.of("Set time to " + time);
        } catch (Exception ignored) {}

        //otherwise, try to parse it as a time of day
        return switch (value) {
            case "sunrise" -> {
                world.setTime(0);
                yield Text.of("Set time to sunrise");
            }
            case "day" -> {
                world.setTime(1000);
                yield Text.of("Set time to day");
            }
            case "noon" -> {
                world.setTime(6000);
                yield Text.of("Set time to noon");
            }
            case "day_end" -> {
                world.setTime(11000);
                yield Text.of("Set time to end of the day");
            }
            case "sunset" -> {
                world.setTime(12000);
                yield Text.of("Set time to sunset");
            }
            case "night" -> {
                world.setTime(13000);
                yield Text.of("Set time to night");
            }
            case "midnight" -> {
                world.setTime(18000);
                yield Text.of("Set time to midnight");
            }
            case "night_end" -> {
                world.setTime(23000);
                yield Text.of("Set time to end of the night");
            }
            default -> Text.of("Failed to execute command, invalid argument: " + value).withStyle(ERROR_STYLE);
        };
    }

    private Text timeAdd(World world, Stack<String> args) {
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String value = args.pop();
        try {
            long time = Long.parseLong(value);
            world.setTime(world.getTime() + time);
            return Text.of("Added " + time + " to the current time");
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid argument: " + value).withStyle(ERROR_STYLE);
        }
    }
}
