package cinnamon.commands;

import cinnamon.registry.MaterialRegistry;
import cinnamon.text.Text;
import cinnamon.utils.AABB;
import cinnamon.utils.Pair;
import cinnamon.world.entity.Entity;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.RoundingMode;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static cinnamon.commands.CommandParser.ERROR_STYLE;

public class Fill implements Command {

    @Override
    public Text execute(Entity source, String[] args) {
        int i = 0;

        //parse position A
        Pair<Vector3f, Integer> pos1 = CommandParser.parseCoordinate(source, args, i);
        if (pos1.second() == 0)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);
        else if (pos1.second() < 0)
            return Text.of("Failed to execute command, invalid argument: " + args[-pos1.second() - 1 + i]).withStyle(ERROR_STYLE);

        i += pos1.second();
        Vector3f posA = pos1.first();

        //parse position B
        Pair<Vector3f, Integer> pos2 = CommandParser.parseCoordinate(source, args, i);
        if (pos2.second() == 0)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);
        else if (pos2.second() < 0)
            return Text.of("Failed to execute command, invalid argument: " + args[-pos2.second() - 1 + i]).withStyle(ERROR_STYLE);

        i += pos2.second();
        Vector3f posB = pos2.first();

        //parse material type
        if (i >= args.length)
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String mat = args[i];
        Vector3i a = new Vector3i(posA, RoundingMode.TRUNCATE);
        Vector3i b = new Vector3i(posB, RoundingMode.TRUNCATE);
        Vector3i min = a.min(b, new Vector3i());
        Vector3i max = a.max(b, new Vector3i());

        //fill with air just removes the terrain
        if (mat.equalsIgnoreCase("air")) {
            source.getWorld().removeTerrain(new AABB(min.x, min.y, min.z, max.x, max.y, max.z).translate(0.5f, 0.5f, 0.5f));
            return Text.of("Filled from %d %d %d to %d %d %d with %s".formatted(min.x, min.y, min.z, max.x, max.y, max.z, "air"));
        }

        //try to actually fill with something
        try {
            MaterialRegistry material = MaterialRegistry.valueOf(mat.toUpperCase());
            TerrainGenerator.fill(source.getWorld(), min.x, min.y, min.z, max.x, max.y, max.z, material);
            return Text.of("Filled from %d %d %d to %d %d %d with %s".formatted(min.x, min.y, min.z, max.x, max.y, max.z, material.name()));
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid material: " + mat).withStyle(ERROR_STYLE);
        }
    }
}
