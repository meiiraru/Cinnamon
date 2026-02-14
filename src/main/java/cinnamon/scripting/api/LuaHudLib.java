package cinnamon.scripting.api;

import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Lua "hud" library - manage custom HUD overlays (text, progress bars).
 * Custom HUD elements are stored by ID and rendered by the LuaWorld's HUD.
 */
public class LuaHudLib extends LuaTable {

    private final WorldClient world;
    private final Map<String, HudElement> elements = new HashMap<>();

    public LuaHudLib(WorldClient world) {
        this.world = world;

        // Set custom text overlay
        set("setText", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String id = args.checkjstring(1);
                int x = args.checkint(2);
                int y = args.checkint(3);
                String text = args.checkjstring(4);
                int color = args.narg() >= 5 ? args.checkint(5) : 0xFFFFFFFF;

                elements.put(id, new HudText(x, y, text, color));
                return LuaValue.TRUE;
            }
        });

        // Set custom progress bar
        set("setProgressBar", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String id = args.checkjstring(1);
                int x = args.checkint(2);
                int y = args.checkint(3);
                int width = args.checkint(4);
                int height = args.checkint(5);
                float progress = (float) args.checkdouble(6);
                int color = args.narg() >= 7 ? args.checkint(7) : 0xFF00FF00;

                elements.put(id, new HudProgressBar(x, y, width, height, progress, color));
                return LuaValue.TRUE;
            }
        });

        // Remove a HUD element
        set("remove", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                elements.remove(arg.checkjstring());
                return LuaValue.TRUE;
            }
        });

        // Clear all custom HUD elements
        set("clear", new ZeroArgFunction() {
            @Override public LuaValue call() {
                elements.clear();
                return LuaValue.TRUE;
            }
        });
    }

    public Map<String, HudElement> getElements() {
        return elements;
    }

    // -- HUD element types --

    public interface HudElement {
        int getX();
        int getY();
    }

    public static class HudText implements HudElement {
        public final int x, y;
        public final String text;
        public final int color;

        public HudText(int x, int y, String text, int color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }

        @Override public int getX() { return x; }
        @Override public int getY() { return y; }
    }

    public static class HudProgressBar implements HudElement {
        public final int x, y, width, height;
        public final float progress;
        public final int color;

        public HudProgressBar(int x, int y, int width, int height, float progress, int color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.progress = progress;
            this.color = color;
        }

        @Override public int getX() { return x; }
        @Override public int getY() { return y; }
    }
}
