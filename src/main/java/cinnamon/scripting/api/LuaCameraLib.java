package cinnamon.scripting.api;

import cinnamon.render.Camera;
import cinnamon.render.WorldRenderer;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Lua "camera" library - get camera position, rotation, and mode.
 */
public class LuaCameraLib extends LuaTable {

    private final WorldClient world;

    public LuaCameraLib(WorldClient world) {
        this.world = world;

        set("getPos", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Camera camera = WorldRenderer.camera;
                if (camera == null) return LuaValue.NIL;
                LuaTable t = new LuaTable();
                t.set("x", LuaValue.valueOf(camera.getPos().x));
                t.set("y", LuaValue.valueOf(camera.getPos().y));
                t.set("z", LuaValue.valueOf(camera.getPos().z));
                return t;
            }
        });

        set("getMode", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.getCameraMode());
            }
        });

        set("isThirdPerson", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(world.isThirdPerson());
            }
        });
    }
}
