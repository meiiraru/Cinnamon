package cinnamon.commands;

import cinnamon.math.collision.shape.AABB;
import cinnamon.model.material.Material;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.text.Text;
import cinnamon.world.entity.Entity;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.RoundingMode;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Stack;
import java.util.function.Supplier;

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

        //parse terrain
        if (args.isEmpty())
            return Text.of("Failed to execute command, missing arguments").withStyle(ERROR_STYLE);

        String terrain = args.pop().toUpperCase();
        Supplier<Terrain> terrainSupplier;
        try {
            terrainSupplier = TerrainRegistry.valueOf(terrain).getFactory();
        } catch (Exception e) {
            return Text.of("Failed to execute command, invalid terrain: " + terrain).withStyle(ERROR_STYLE);
        }

        //parse material type
        Material material;
        String matName;
        boolean remove;

        if (args.isEmpty()) {
            //if no material is specified, use the default material for the terrain
            material = null;
            matName = "default";
            remove = false;
        } else {
            //try to parse the material type
            String mat = args.pop();
            Integer matID = CommandParser.parseMaterial(mat);

            if (matID == null)
                return Text.of("Failed to execute command, invalid material: " + mat).withStyle(ERROR_STYLE);

            //if the material ID is -1, it means air, so we will remove the terrain instead of filling it
            if (matID == -1) {
                material = null;
                matName = "air";
                remove = true;
            } else {
                MaterialRegistry matReg = MaterialRegistry.values()[matID];
                material = matReg.material;
                matName = matReg.name();
                remove = false;
            }
        }

        //apply the fill
        Vector3i a = new Vector3i(pos1, RoundingMode.TRUNCATE);
        Vector3i b = new Vector3i(pos2, RoundingMode.TRUNCATE);
        Vector3i min = a.min(b, new Vector3i());
        Vector3i max = a.max(b, new Vector3i());

        //clear previous terrain at the position
        source.getWorld().removeTerrain(new AABB(min.x, min.y, min.z, max.x, max.y, max.z).translate(0.5f, 0.5f, 0.5f));

        //fill with air just removes the terrain
        if (remove)
            return Text.of("Filled from %d %d %d to %d %d %d with %s".formatted(min.x, min.y, min.z, max.x, max.y, max.z, matName));

        //otherwise actually fill with something
        TerrainGenerator.fill(source.getWorld(), min.x, min.y, min.z, max.x, max.y, max.z, terrainSupplier, material);
        return Text.of("Filled from %d %d %d to %d %d %d with %s %s".formatted(min.x, min.y, min.z, max.x, max.y, max.z, matName, terrain));
    }

    @Override
    public Text getHelpCommand() {
        return Text.of("Usage: /fill <pos1> <pos2> <model> [<material>]")
                .append("\n")
                .append("Fills a region defined by two positions with the specified model and material");
    }
}
