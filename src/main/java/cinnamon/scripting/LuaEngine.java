package cinnamon.scripting;

import cinnamon.logger.Logger;
import cinnamon.scripting.api.*;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.compiler.LuaC;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Core Lua scripting engine for Cinnamon.
 * Manages a sandboxed LuaJ Globals instance with custom Cinnamon API bindings.
 */
public class LuaEngine {

    public static final Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/lua");

    private Globals globals;
    private WorldClient world;
    private final List<Consumer<Object[]>> registeredEventCallbacks = new ArrayList<>();
    private Consumer<String> outputConsumer;

    public LuaEngine(WorldClient world) {
        this.world = world;
        initGlobals();
    }

    private void initGlobals() {
        globals = new Globals();

        // Install safe base libraries (no luajava, no os.execute, no io)
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new StringLib());
        globals.load(new TableLib());
        globals.load(new JseMathLib());

        // Install compiler so we can load scripts from source
        LuaC.install(globals);

        // Sandbox: remove dangerous functions
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("collectgarbage", LuaValue.NIL);

        // Override print to redirect output
        globals.set("print", new LuaPrintFunction(this));

        // Install Cinnamon API libraries
        installCinnamonAPIs();
    }

    private void installCinnamonAPIs() {
        globals.set("world", new LuaWorldLib(world));
        globals.set("entity", new LuaEntityLib(world));
        globals.set("terrain", new LuaTerrainLib(world));
        globals.set("particles", new LuaParticleLib(world));
        globals.set("lights", new LuaLightLib(world));
        globals.set("sound", new LuaSoundLib(world));
        globals.set("events", new LuaEventsLib(world, this));
        globals.set("commands", new LuaCommandsLib(world));
        globals.set("registry", new LuaRegistryLib());
        globals.set("hud", new LuaHudLib(world));
        globals.set("camera", new LuaCameraLib(world));
        globals.set("player", new LuaPlayerLib(world));

        // Add script loading function
        globals.set("runScript", new RunScriptFunction(this));
    }

    /**
     * Execute a Lua string and return the result as a string (or error).
     */
    public String execute(String code) {
        try {
            LuaValue chunk = globals.load(code, "console");
            Varargs result = chunk.invoke();
            if (result == LuaValue.NONE || result == LuaValue.NIL || result.narg() == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= result.narg(); i++) {
                if (i > 1) sb.append("\t");
                sb.append(result.arg(i).tojstring());
            }
            return sb.toString();
        } catch (LuaError e) {
            String msg = e.getMessage();
            LOGGER.error("Lua error: " + msg);
            return "Error: " + msg;
        }
    }

    /**
     * Load and execute a Lua script from the classpath resources.
     */
    public String executeResource(String resourcePath) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("resources/vanilla/scripts/" + resourcePath);
            if (is == null) {
                return "Error: Script not found: " + resourcePath;
            }
            LuaValue chunk = globals.load(is, resourcePath, "t", globals);
            Varargs result = chunk.invoke();
            is.close();
            if (result == LuaValue.NONE || result == LuaValue.NIL || result.narg() == 0) {
                return null;
            }
            return result.arg1().tojstring();
        } catch (LuaError e) {
            LOGGER.error("Lua script error in " + resourcePath + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            LOGGER.error("Failed to load script: " + resourcePath, e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Send output to the console.
     */
    public void print(String text) {
        LOGGER.info("[Lua] " + text);
        if (outputConsumer != null) {
            outputConsumer.accept(text);
        }
    }

    public void setOutputConsumer(Consumer<String> consumer) {
        this.outputConsumer = consumer;
    }

    public Globals getGlobals() {
        return globals;
    }

    public WorldClient getWorld() {
        return world;
    }

    public List<Consumer<Object[]>> getRegisteredEventCallbacks() {
        return registeredEventCallbacks;
    }

    /**
     * Clean up Lua state.
     */
    public void close() {
        registeredEventCallbacks.clear();
        globals = null;
    }

    // -- Inner function classes --

    /**
     * Custom print function that redirects to the Lua console.
     */
    private static class LuaPrintFunction extends VarArgFunction {
        private final LuaEngine engine;

        LuaPrintFunction(LuaEngine engine) {
            this.engine = engine;
        }

        @Override
        public Varargs invoke(Varargs args) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= args.narg(); i++) {
                if (i > 1) sb.append("\t");
                sb.append(args.arg(i).tojstring());
            }
            engine.print(sb.toString());
            return LuaValue.NONE;
        }
    }

    /**
     * Function to run a script from the scripts directory.
     * Usage: runScript("examples/hello_world.lua")
     */
    private static class RunScriptFunction extends VarArgFunction {
        private final LuaEngine engine;

        RunScriptFunction(LuaEngine engine) {
            this.engine = engine;
        }

        @Override
        public Varargs invoke(Varargs args) {
            String result = engine.executeResource(args.checkjstring(1));
            if (result != null) {
                engine.print(result);
            }
            return LuaValue.NIL;
        }
    }
}
