package cinnamon.scripting.wrappers;

import cinnamon.sound.SoundInstance;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Wraps a SoundInstance as a LuaUserdata with a metatable exposing sound control methods.
 */
public class LuaSoundWrapper {

    public static LuaValue wrap(SoundInstance sound) {
        LuaTable metatable = new LuaTable();
        LuaTable index = new LuaTable();

        // -- Playback controls --

        index.set("play", new ZeroArgFunction() {
            @Override public LuaValue call() {
                sound.play();
                return LuaValue.NIL;
            }
        });

        index.set("pause", new ZeroArgFunction() {
            @Override public LuaValue call() {
                sound.pause();
                return LuaValue.NIL;
            }
        });

        index.set("stop", new ZeroArgFunction() {
            @Override public LuaValue call() {
                sound.stop();
                return LuaValue.NIL;
            }
        });

        // -- State --

        index.set("isPlaying", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(sound.isPlaying());
            }
        });

        index.set("isPaused", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(sound.isPaused());
            }
        });

        index.set("isStopped", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(sound.isStopped());
            }
        });

        // -- Properties --

        index.set("setPitch", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                sound.pitch((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("setVolume", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                sound.volume((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("getVolume", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(sound.getVolume());
            }
        });

        index.set("setLoop", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                sound.loop(arg.checkboolean());
                return LuaValue.NIL;
            }
        });

        index.set("setMaxDistance", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                sound.maxDistance((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("setDistance", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                sound.distance((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });

        // -- Playback time --

        index.set("getPlaybackTime", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(sound.getPlaybackTime());
            }
        });

        index.set("setPlaybackTime", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                sound.setPlaybackTime(arg.checklong());
                return LuaValue.NIL;
            }
        });

        // -- Category --

        index.set("getCategory", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(sound.getCategory().name());
            }
        });

        // -- Metatable --

        metatable.set("__index", index);
        metatable.set("__tostring", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf("Sound[" + sound.getCategory().name() + "]");
            }
        });

        LuaUserdata userdata = new LuaUserdata(sound);
        userdata.setmetatable(metatable);
        return userdata;
    }
}
