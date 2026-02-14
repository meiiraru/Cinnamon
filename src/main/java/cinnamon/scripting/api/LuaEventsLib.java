package cinnamon.scripting.api;

import cinnamon.events.EventType;
import cinnamon.scripting.LuaEngine;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import java.util.function.Consumer;

/**
 * Lua "events" library - register callbacks for game events and delayed execution.
 */
public class LuaEventsLib extends LuaTable {

    private final WorldClient world;
    private final LuaEngine engine;

    public LuaEventsLib(WorldClient world, LuaEngine engine) {
        this.world = world;
        this.engine = engine;

        // Register event callback
        set("on", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue eventNameArg, LuaValue callbackArg) {
                String eventName = eventNameArg.checkjstring().toUpperCase();
                LuaFunction callback = callbackArg.checkfunction();

                EventType type = parseEventType(eventName);
                if (type == null) {
                    return LuaValue.error("Unknown event type: " + eventName);
                }

                Consumer<Object[]> consumer = args -> {
                    try {
                        LuaTable luaArgs = new LuaTable();
                        for (int i = 0; i < args.length; i++) {
                            luaArgs.set(i + 1, coerceToLua(args[i]));
                        }
                        callback.call(luaArgs);
                    } catch (LuaError e) {
                        engine.print("Event error (" + eventName + "): " + e.getMessage());
                    }
                };

                cinnamon.Client.getInstance().events.registerEvent(type, consumer);
                engine.getRegisteredEventCallbacks().add(consumer);

                return LuaValue.TRUE;
            }
        });

        // Delayed execution (in ticks)
        set("after", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue ticksArg, LuaValue callbackArg) {
                int ticks = ticksArg.checkint();
                LuaFunction callback = callbackArg.checkfunction();

                // Use world scheduledTicks with countdown
                scheduleDelayed(ticks, callback);

                return LuaValue.TRUE;
            }
        });

        // Run callback every N ticks
        set("every", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue intervalArg, LuaValue callbackArg) {
                int interval = intervalArg.checkint();
                LuaFunction callback = callbackArg.checkfunction();

                scheduleRepeating(interval, callback);

                return LuaValue.TRUE;
            }
        });

        // List all event types
        set("types", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable table = new LuaTable();
                EventType[] values = EventType.values();
                for (int i = 0; i < values.length; i++) {
                    table.set(i + 1, LuaValue.valueOf(values[i].name()));
                }
                return table;
            }
        });
    }

    private void scheduleDelayed(int ticksRemaining, LuaFunction callback) {
        world.scheduleTick(() -> {
            if (ticksRemaining <= 0) {
                try {
                    callback.call();
                } catch (LuaError e) {
                    engine.print("Delayed callback error: " + e.getMessage());
                }
            } else {
                scheduleDelayed(ticksRemaining - 1, callback);
            }
        });
    }

    private void scheduleRepeating(int interval, LuaFunction callback) {
        final int[] counter = {0};
        Consumer<Object[]> tickConsumer = args -> {
            counter[0]++;
            if (counter[0] >= interval) {
                counter[0] = 0;
                try {
                    callback.call();
                } catch (LuaError e) {
                    engine.print("Repeating callback error: " + e.getMessage());
                }
            }
        };
        cinnamon.Client.getInstance().events.registerEvent(EventType.TICK_BEFORE_WORLD, tickConsumer);
        engine.getRegisteredEventCallbacks().add(tickConsumer);
    }

    private static EventType parseEventType(String name) {
        try {
            return EventType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LuaValue coerceToLua(Object obj) {
        if (obj == null) return LuaValue.NIL;
        if (obj instanceof Integer i) return LuaValue.valueOf(i);
        if (obj instanceof Long l) return LuaValue.valueOf(l);
        if (obj instanceof Float f) return LuaValue.valueOf(f);
        if (obj instanceof Double d) return LuaValue.valueOf(d);
        if (obj instanceof Boolean b) return LuaValue.valueOf(b);
        if (obj instanceof String s) return LuaValue.valueOf(s);
        return LuaValue.valueOf(obj.toString());
    }
}
