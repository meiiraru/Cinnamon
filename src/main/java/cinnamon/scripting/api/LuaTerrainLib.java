package cinnamon.scripting.api;

import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.utils.AABB;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import cinnamon.world.worldgen.TerrainGenerator;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.joml.Vector3f;

/**
 * Lua "terrain" library - place, remove, and fill terrain.
 */
public class LuaTerrainLib extends LuaTable {

    private final WorldClient world;

    public LuaTerrainLib(WorldClient world) {
        this.world = world;

        // Place a single terrain block
        set("place", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String typeName = args.narg() >= 1 ? args.checkjstring(1).toUpperCase() : "BOX";
                float x = (float) args.checkdouble(2);
                float y = (float) args.checkdouble(3);
                float z = (float) args.checkdouble(4);
                String materialName = args.narg() >= 5 ? args.checkjstring(5).toUpperCase() : "DEFAULT";
                int rotation = args.narg() >= 6 ? args.checkint(6) : 0;

                TerrainRegistry type = parseTerrainType(typeName);
                MaterialRegistry material = parseMaterial(materialName);

                if (type == null) return LuaValue.error("Unknown terrain type: " + typeName);
                if (type.getFactory() == null) return LuaValue.error("Terrain type has no factory: " + typeName);
                if (material == null) return LuaValue.error("Unknown material: " + materialName);

                world.scheduleTick(() -> {
                    Terrain t = type.getFactory().get();
                    t.setPos(x, y, z);
                    t.setMaterial(material);
                    t.setRotation((byte) (rotation % 4));
                    world.addTerrain(t);
                });

                return LuaValue.TRUE;
            }
        });

        // Remove terrain at position
        set("remove", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                float x = (float) args.checkdouble(1);
                float y = (float) args.checkdouble(2);
                float z = (float) args.checkdouble(3);
                float size = args.narg() >= 4 ? (float) args.checkdouble(4) : 0.5f;

                world.scheduleTick(() -> {
                    AABB area = new AABB().set(new Vector3f(x, y, z)).inflate(size);
                    world.removeTerrain(area);
                });

                return LuaValue.TRUE;
            }
        });

        // Fill a region with terrain
        set("fill", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                int x1 = args.checkint(1);
                int y1 = args.checkint(2);
                int z1 = args.checkint(3);
                int x2 = args.checkint(4);
                int y2 = args.checkint(5);
                int z2 = args.checkint(6);
                String materialName = args.narg() >= 7 ? args.checkjstring(7).toUpperCase() : "DEFAULT";

                MaterialRegistry material = parseMaterial(materialName);
                if (material == null) return LuaValue.error("Unknown material: " + materialName);

                world.scheduleTick(() -> TerrainGenerator.fill(world, x1, y1, z1, x2, y2, z2, material));

                return LuaValue.TRUE;
            }
        });

        // Generate a Menger sponge
        set("mengerSponge", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                int level = args.checkint(1);
                int x = args.checkint(2);
                int y = args.checkint(3);
                int z = args.checkint(4);
                String materialName = args.narg() >= 5 ? args.checkjstring(5).toUpperCase() : "GOLD";

                MaterialRegistry material = parseMaterial(materialName);
                if (material == null) return LuaValue.error("Unknown material: " + materialName);

                world.scheduleTick(() -> TerrainGenerator.generateMengerSponge(world, level, x, y, z, material));

                return LuaValue.TRUE;
            }
        });

        // List all terrain types
        set("types", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable table = new LuaTable();
                TerrainRegistry[] values = TerrainRegistry.values();
                for (int i = 0; i < values.length; i++) {
                    table.set(i + 1, LuaValue.valueOf(values[i].name()));
                }
                return table;
            }
        });

        // List all materials
        set("materials", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable table = new LuaTable();
                MaterialRegistry[] values = MaterialRegistry.values();
                for (int i = 0; i < values.length; i++) {
                    table.set(i + 1, LuaValue.valueOf(values[i].name()));
                }
                return table;
            }
        });
    }

    private static TerrainRegistry parseTerrainType(String name) {
        try {
            return TerrainRegistry.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static MaterialRegistry parseMaterial(String name) {
        try {
            return MaterialRegistry.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
