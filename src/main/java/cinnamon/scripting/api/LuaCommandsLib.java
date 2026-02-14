package cinnamon.scripting.api;

import cinnamon.commands.Command;
import cinnamon.commands.CommandParser;
import cinnamon.scripting.wrappers.LuaEntityWrapper;
import cinnamon.world.entity.Entity;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Lua "commands" library - register and execute chat commands from Lua.
 */
public class LuaCommandsLib extends LuaTable {

    private final WorldClient world;
    private final List<String> registeredCommands = new ArrayList<>();

    public LuaCommandsLib(WorldClient world) {
        this.world = world;

        // Register a custom command
        set("register", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String name = args.checkjstring(1).toLowerCase();
                LuaFunction callback = args.checkfunction(2);

                // Create a Command that calls the Lua function
                Command command = (Entity source, String[] cmdArgs) -> {
                    try {
                        LuaTable luaArgs = new LuaTable();
                        for (int i = 0; i < cmdArgs.length; i++) {
                            luaArgs.set(i + 1, LuaValue.valueOf(cmdArgs[i]));
                        }
                        LuaValue luaSource = source != null ? LuaEntityWrapper.wrap(source) : LuaValue.NIL;
                        callback.call(luaSource, luaArgs);
                    } catch (LuaError e) {
                        cinnamon.scripting.LuaEngine.LOGGER.error("Lua command error: " + e.getMessage());
                    }
                };

                // Register via CommandParser (using reflection to access the trie)
                try {
                    var field = CommandParser.class.getDeclaredField("commandTrie");
                    field.setAccessible(true);
                    var trie = (cinnamon.utils.Trie<Command>) field.get(null);
                    trie.insert(name, command);
                    registeredCommands.add(name);
                } catch (Exception e) {
                    return LuaValue.error("Failed to register command: " + e.getMessage());
                }

                return LuaValue.TRUE;
            }
        });

        // Execute a command as the player
        set("execute", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                String input = arg.checkjstring();
                if (world.player != null) {
                    CommandParser.parseCommand(world.player, input);
                }
                return LuaValue.NIL;
            }
        });
    }

    public List<String> getRegisteredCommands() {
        return registeredCommands;
    }
}
