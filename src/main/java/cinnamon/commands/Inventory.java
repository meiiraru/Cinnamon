package cinnamon.commands;

import cinnamon.text.Text;
import cinnamon.world.entity.collectable.ItemEntity;
import cinnamon.world.entity.living.LivingEntity;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Inventory implements Command {
    @Override
    public Text execute(CommandSource source, Stack<String> args) {
        //grab inventory from source entity
        cinnamon.world.items.Inventory inv;
        if (source.entity() instanceof LivingEntity le)
            inv = le.getInventory();
        else
            return Text.of("Entity has no inventory").withStyle(ERROR_STYLE);

        //find action to perform
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String action = args.pop().toLowerCase();
        switch (action) {
            //clear inventory
            case "clear" -> {
                for (int i = 0; i < inv.getSize(); i++)
                    inv.setItem(i, null);
                return Text.of("Inventory cleared");
            }
            //drop item from inventory
            case "drop" -> {
                if (args.isEmpty())
                    return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

                //parse index
                String indexStr = args.pop();
                int index;
                try {
                    index = Integer.parseInt(indexStr);
                } catch (NumberFormatException e) {
                    return Text.of("Failed to execute command, invalid argument: " + indexStr).withStyle(ERROR_STYLE);
                }

                //parse count
                int count = -1;
                if (!args.isEmpty()) {
                    String countStr = args.pop();
                    try {
                        count = Integer.parseInt(countStr);
                    } catch (NumberFormatException e) {
                        return Text.of("Failed to execute command, invalid argument: " + countStr).withStyle(ERROR_STYLE);
                    }
                }

                if (count == 0)
                    return Text.of("Failed to execute command, invalid argument: count cannot be 0").withStyle(ERROR_STYLE);

                //drop item
                ItemEntity itemEntity = le.dropItem(index, count);

                //result
                if (itemEntity == null)
                    return Text.of("No item to drop in slot " + index).withStyle(ERROR_STYLE);

                String indexName = index < 0 ? "selected slot" : "slot " + index;
                return Text.of("Dropped " + itemEntity.getItem().getCount() + " items from the " + indexName);
            }
        }

        return null;
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /inventory [clear|drop] <index> <count>")
                .append("\n")
                .append("Clears or drops items from your inventory");
    }
}
