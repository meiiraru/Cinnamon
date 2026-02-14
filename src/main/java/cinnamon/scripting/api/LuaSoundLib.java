package cinnamon.scripting.api;

import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundInstance;
import cinnamon.sound.SoundManager;
import cinnamon.scripting.wrappers.LuaSoundWrapper;
import cinnamon.utils.Resource;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.joml.Vector3f;

/**
 * Lua "sound" library - play, stop, and manage sounds.
 */
public class LuaSoundLib extends LuaTable {

    private final WorldClient world;

    public LuaSoundLib(WorldClient world) {
        this.world = world;

        // Play a sound
        set("play", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String resource = args.checkjstring(1);
                String categoryName = args.narg() >= 2 ? args.checkjstring(2).toUpperCase() : "MISC";
                float x = args.narg() >= 3 ? (float) args.checkdouble(3) : 0f;
                float y = args.narg() >= 4 ? (float) args.checkdouble(4) : 0f;
                float z = args.narg() >= 5 ? (float) args.checkdouble(5) : 0f;

                SoundCategory category = parseCategory(categoryName);
                if (category == null) category = SoundCategory.MISC;

                SoundInstance instance = world.playSound(new Resource(resource), category, new Vector3f(x, y, z));
                if (instance == null) return LuaValue.NIL;
                return LuaSoundWrapper.wrap(instance);
            }
        });

        // Stop all sounds
        set("stopAll", new ZeroArgFunction() {
            @Override public LuaValue call() {
                SoundManager.stopAll(c -> c != SoundCategory.GUI && c != SoundCategory.MASTER);
                return LuaValue.NIL;
            }
        });

        // Pause all sounds
        set("pauseAll", new ZeroArgFunction() {
            @Override public LuaValue call() {
                SoundManager.pauseAll(c -> c != SoundCategory.GUI && c != SoundCategory.MASTER);
                return LuaValue.NIL;
            }
        });

        // Resume all sounds
        set("resumeAll", new ZeroArgFunction() {
            @Override public LuaValue call() {
                SoundManager.resumeAll(c -> c != SoundCategory.GUI && c != SoundCategory.MASTER);
                return LuaValue.NIL;
            }
        });

        // List sound categories
        set("categories", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable table = new LuaTable();
                SoundCategory[] values = SoundCategory.values();
                for (int i = 0; i < values.length; i++) {
                    table.set(i + 1, LuaValue.valueOf(values[i].name()));
                }
                return table;
            }
        });
    }

    private static SoundCategory parseCategory(String name) {
        try {
            return SoundCategory.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
