package cinnamon.scripting.api;

import cinnamon.scripting.wrappers.LuaLightWrapper;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.light.Spotlight;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Lua "lights" library - add, modify and remove lights.
 */
public class LuaLightLib extends LuaTable {

    private final WorldClient world;

    public LuaLightLib(WorldClient world) {
        this.world = world;

        // Add a light
        set("add", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue typeArg, LuaValue optsArg) {
                String typeName = typeArg.checkjstring().toUpperCase();
                LuaTable opts = optsArg.istable() ? optsArg.checktable() : new LuaTable();

                Light light = createLight(typeName);
                if (light == null) {
                    return LuaValue.error("Unknown light type: " + typeName);
                }

                // Apply options
                applyOptions(light, typeName, opts);

                world.addLight(light);
                return LuaLightWrapper.wrap(light, world);
            }
        });

        // Add a point light (convenience)
        set("addPoint", new OneArgFunction() {
            @Override public LuaValue call(LuaValue optsArg) {
                LuaTable opts = optsArg.istable() ? optsArg.checktable() : new LuaTable();
                PointLight light = new PointLight();
                applyOptions(light, "POINT", opts);
                world.addLight(light);
                return LuaLightWrapper.wrap(light, world);
            }
        });

        // Add a spotlight (convenience)
        set("addSpot", new OneArgFunction() {
            @Override public LuaValue call(LuaValue optsArg) {
                LuaTable opts = optsArg.istable() ? optsArg.checktable() : new LuaTable();
                Spotlight light = new Spotlight();
                applyOptions(light, "SPOT", opts);
                world.addLight(light);
                return LuaLightWrapper.wrap(light, world);
            }
        });

        // List light types
        set("types", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable table = new LuaTable();
                Light.Type[] values = Light.Type.values();
                for (int i = 0; i < values.length; i++) {
                    table.set(i + 1, LuaValue.valueOf(values[i].name()));
                }
                return table;
            }
        });
    }

    private Light createLight(String typeName) {
        return switch (typeName) {
            case "POINT" -> new PointLight();
            case "SPOT", "SPOTLIGHT" -> new Spotlight();
            case "DIRECTIONAL" -> new DirectionalLight();
            default -> null;
        };
    }

    private void applyOptions(Light light, String typeName, LuaTable opts) {
        // Position
        if (!opts.get("x").isnil()) {
            float x = (float) opts.get("x").checkdouble();
            float y = (float) opts.get("y").checkdouble();
            float z = (float) opts.get("z").checkdouble();
            light.pos(x, y, z);
        } else if (!opts.get("pos").isnil() && opts.get("pos").istable()) {
            LuaTable pos = opts.get("pos").checktable();
            light.pos((float) pos.get(1).checkdouble(), (float) pos.get(2).checkdouble(), (float) pos.get(3).checkdouble());
        }

        // Direction
        if (!opts.get("dirX").isnil()) {
            float dx = (float) opts.get("dirX").checkdouble();
            float dy = (float) opts.get("dirY").checkdouble();
            float dz = (float) opts.get("dirZ").checkdouble();
            light.direction(dx, dy, dz);
        } else if (!opts.get("direction").isnil() && opts.get("direction").istable()) {
            LuaTable dir = opts.get("direction").checktable();
            light.direction((float) dir.get(1).checkdouble(), (float) dir.get(2).checkdouble(), (float) dir.get(3).checkdouble());
        }

        // Color
        if (!opts.get("color").isnil()) {
            light.color(opts.get("color").checkint());
        }

        // Intensity
        if (!opts.get("intensity").isnil()) {
            light.intensity((float) opts.get("intensity").checkdouble());
        }

        // Shadows
        if (!opts.get("castsShadows").isnil()) {
            light.castsShadows(opts.get("castsShadows").checkboolean());
        }

        // Glare
        if (!opts.get("glareSize").isnil()) {
            light.glareSize((float) opts.get("glareSize").checkdouble());
        }
        if (!opts.get("glareIntensity").isnil()) {
            light.glareIntensity((float) opts.get("glareIntensity").checkdouble());
        }

        // Point light specific
        if (light instanceof PointLight pointLight) {
            if (!opts.get("falloffStart").isnil() && !opts.get("falloffEnd").isnil()) {
                pointLight.falloff((float) opts.get("falloffStart").checkdouble(), (float) opts.get("falloffEnd").checkdouble());
            } else if (!opts.get("falloff").isnil()) {
                pointLight.falloff((float) opts.get("falloff").checkdouble());
            }
        }

        // Spotlight specific
        if (light instanceof Spotlight spotlight) {
            if (!opts.get("innerAngle").isnil() && !opts.get("outerAngle").isnil()) {
                spotlight.angle((float) opts.get("innerAngle").checkdouble(), (float) opts.get("outerAngle").checkdouble());
            } else if (!opts.get("angle").isnil()) {
                spotlight.angle((float) opts.get("angle").checkdouble());
            }
            if (!opts.get("beamStrength").isnil()) {
                spotlight.beamStrength((float) opts.get("beamStrength").checkdouble());
            }
        }
    }
}
