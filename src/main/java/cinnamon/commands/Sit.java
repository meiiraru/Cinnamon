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
    public Text execute(CommandSource source, Stack<String> args) {
        //check for entity
        Entity self = source.entity();
        if (self == null)
            return Text.of("Target not found").withStyle(ERROR_STYLE);

        if (self.isRiding()) {
            self.stopRiding();
            return Text.of("Stopped sitting");
        }

        SeatEntity seatEntity = new SeatEntity(UUID.randomUUID());
        seatEntity.setPos(source.position());
        seatEntity.setRot(0f, Maths.getYaw(source.rotation()), 0f);
        source.world().addEntity(seatEntity);
        seatEntity.addRider(self);

        return Text.of("Sat down");
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /sit")
                .append("\n")
                .append("Sits down on the ground at the current position");
    }
}
