package cinnamon.scripting.api;

import cinnamon.utils.AABB;
import cinnamon.world.entity.Entity;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import cinnamon.scripting.wrappers.LuaEntityWrapper;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * Lua "world" library - provides access to world state and operations.
 */
public class LuaWorldLib extends LuaTable {

    private final WorldClient world;

    public LuaWorldLib(WorldClient world) {
        this.world = world;

        // Time
        set("getTime", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.getTime());
            }
        });

        set("setTime", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                // Access via reflection or scheduled tick since worldTime is protected
                world.scheduleTick(() -> {
                    try {
                        var field = world.getClass().getSuperclass().getDeclaredField("worldTime");
                        field.setAccessible(true);
                        field.setLong(world, arg.checklong());
                    } catch (Exception e) {
                        // fallback - ignore
                    }
                });
                return LuaValue.NIL;
            }
        });

        set("getDay", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.getDay());
            }
        });

        set("getTimeOfDay", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.getTimeOfTheDay());
            }
        });

        set("getTimeProgress", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.getTimeOfDayProgress());
            }
        });

        // Pause
        set("setPaused", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                world.setPaused(arg.checkboolean());
                return LuaValue.NIL;
            }
        });

        set("isPaused", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.isPaused());
            }
        });

        // Gravity
        set("getGravity", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.gravity);
            }
        });

        set("setGravity", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                world.gravity = (float) arg.checkdouble();
                return LuaValue.NIL;
            }
        });

        // Entity queries
        set("getEntityByUUID", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Entity e = world.getEntityByUUID(UUID.fromString(arg.checkjstring()));
                return e != null ? LuaEntityWrapper.wrap(e) : LuaValue.NIL;
            }
        });

        set("getEntitiesInBox", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                float x1 = (float) args.checkdouble(1);
                float y1 = (float) args.checkdouble(2);
                float z1 = (float) args.checkdouble(3);
                float x2 = (float) args.checkdouble(4);
                float y2 = (float) args.checkdouble(5);
                float z2 = (float) args.checkdouble(6);
                AABB box = new AABB().set(new Vector3f(x1, y1, z1)).expand(new Vector3f(x2, y2, z2));
                List<Entity> entities = world.getEntities(box);
                LuaTable table = new LuaTable();
                for (int i = 0; i < entities.size(); i++) {
                    table.set(i + 1, LuaEntityWrapper.wrap(entities.get(i)));
                }
                return table;
            }
        });

        // Explosion
        set("explode", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                float x = (float) args.checkdouble(1);
                float y = (float) args.checkdouble(2);
                float z = (float) args.checkdouble(3);
                float radius = (float) args.checkdouble(4);
                float strength = args.narg() >= 5 ? (float) args.checkdouble(5) : radius;
                boolean invisible = args.narg() >= 6 && args.checkboolean(6);
                AABB area = new AABB().set(new Vector3f(x, y, z)).inflate(radius);
                world.scheduleTick(() -> world.explode(area, strength, null, invisible));
                return LuaValue.NIL;
            }
        });

        // Player shortcut
        set("getPlayer", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return world.player != null ? LuaEntityWrapper.wrap(world.player) : LuaValue.NIL;
            }
        });

        // Get entity count
        set("getEntityCount", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.getEntities(new AABB().inflate(10000)).size());
            }
        });
    }
}
