package cinnamon.scripting.api;

import cinnamon.scripting.wrappers.LuaEntityWrapper;
import cinnamon.world.entity.living.Player;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Lua "player" library - convenience access to the local player.
 */
public class LuaPlayerLib extends LuaTable {

    private final WorldClient world;

    public LuaPlayerLib(WorldClient world) {
        this.world = world;

        set("get", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return world.player != null ? LuaEntityWrapper.wrap(world.player) : LuaValue.NIL;
            }
        });

        set("getPos", new ZeroArgFunction() {
            @Override public LuaValue call() {
                if (world.player == null) return LuaValue.NIL;
                LuaTable t = new LuaTable();
                t.set("x", LuaValue.valueOf(world.player.getPos().x));
                t.set("y", LuaValue.valueOf(world.player.getPos().y));
                t.set("z", LuaValue.valueOf(world.player.getPos().z));
                return t;
            }
        });

        set("setPos", new ThreeArgFunction() {
            @Override public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                if (world.player == null) return LuaValue.NIL;
                world.player.setPos((float) x.checkdouble(), (float) y.checkdouble(), (float) z.checkdouble());
                return LuaValue.TRUE;
            }
        });

        set("getHealth", new ZeroArgFunction() {
            @Override public LuaValue call() {
                if (world.player == null) return LuaValue.NIL;
                return LuaValue.valueOf(world.player.getHealth());
            }
        });

        set("setHealth", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                if (world.player == null) return LuaValue.NIL;
                world.player.setHealth(arg.checkint());
                return LuaValue.TRUE;
            }
        });

        set("heal", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                if (world.player == null) return LuaValue.NIL;
                world.player.heal(arg.checkint());
                return LuaValue.TRUE;
            }
        });

        set("setGodMode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                if (world.player == null) return LuaValue.NIL;
                world.player.getAbilities().godMode(arg.checkboolean());
                return LuaValue.TRUE;
            }
        });

        set("setCanFly", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                if (world.player == null) return LuaValue.NIL;
                world.player.getAbilities().canFly(arg.checkboolean());
                return LuaValue.TRUE;
            }
        });

        set("setNoclip", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                if (world.player == null) return LuaValue.NIL;
                world.player.getAbilities().noclip(arg.checkboolean());
                return LuaValue.TRUE;
            }
        });

        set("setCanBuild", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                if (world.player == null) return LuaValue.NIL;
                world.player.getAbilities().canBuild(arg.checkboolean());
                return LuaValue.TRUE;
            }
        });

        set("getName", new ZeroArgFunction() {
            @Override public LuaValue call() {
                if (world.player == null) return LuaValue.NIL;
                return LuaValue.valueOf(world.player.getName());
            }
        });

        set("respawn", new ZeroArgFunction() {
            @Override public LuaValue call() {
                world.scheduleTick(() -> world.respawn(false));
                return LuaValue.TRUE;
            }
        });
    }
}
