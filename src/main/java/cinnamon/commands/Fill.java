package cinnamon.commands;

import cinnamon.math.shape.AABB;
import cinnamon.registry.MaterialRegistry;
import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.RoundingMode;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Stack;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Fill implements Command {

    @Override
    public Text execute(Entity source, Stack<String> args) {
        //parse position A
        if (args.size() < 3)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector3f pos1 = CommandParser.parseCoordinate(source, args);
        if (pos1 == null)
            return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

        //parse position B
        if (args.size() < 3)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector3f pos2 = CommandParser.parseCoordinate(source, args);
        if (pos2 == null)
            return Text.of("Failed to execute command, invalid argument: " + args.peek()).withStyle(ERROR_STYLE);

        //parse material type
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        Vector3i a = new Vector3i(pos1, RoundingMode.TRUNCATE);
        Vector3i b = new Vector3i(pos2, RoundingMode.TRUNCATE);
        Vector3i min = a.min(b, new Vector3i());
        Vector3i max = a.max(b, new Vector3i());

        String mat = args.pop();
        Integer material = CommandParser.parseMaterial(mat);

        if (material == null)
            return Text.of("Failed to execute command, invalid material: " + mat).withStyle(ERROR_STYLE);

        //clear previous terrain at the position
        source.getWorld().removeTerrain(new AABB(min.x, min.y, min.z, max.x, max.y, max.z).translate(0.5f, 0.5f, 0.5f));

        //fill with air just removes the terrain
        if (material == -1)
            return Text.of("Filled from %d %d %d to %d %d %d with %s".formatted(min.x, min.y, min.z, max.x, max.y, max.z, "air"));

        //otherwise actually fill with something
        MaterialRegistry matReg = MaterialRegistry.values()[material];
        TerrainGenerator.fill(source.getWorld(), min.x, min.y, min.z, max.x, max.y, max.z, matReg);
        return Text.of("Filled from %d %d %d to %d %d %d with %s".formatted(min.x, min.y, min.z, max.x, max.y, max.z, matReg.name()));
    }
}
