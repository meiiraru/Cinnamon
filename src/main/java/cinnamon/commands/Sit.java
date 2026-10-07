package cinnamon.commands;

import cinnamon.math.Maths;
import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.misc.SeatEntity;

import java.util.Stack;
import java.util.UUID;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Sit implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        if (source.isRiding())
            return Text.of("Unable to sit while sitting or riding something").withStyle(ERROR_STYLE);

        SeatEntity seatEntity = new SeatEntity(UUID.randomUUID());
        seatEntity.setPos(source.getTransform().getPos());
        seatEntity.setRot(0f, Maths.getYaw(source.getTransform().getRot()), 0f);
        source.getWorld().addEntity(seatEntity);
        seatEntity.addRider(source);

        return Text.of("Sat down");
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /sit")
                .append("\n")
                .append("Sits down on the ground at the current position");
    }
}
