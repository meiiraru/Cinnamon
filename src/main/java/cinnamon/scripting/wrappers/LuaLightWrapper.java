package cinnamon.scripting.wrappers;

import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.light.Spotlight;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Wraps a Light instance as a LuaUserdata with a metatable exposing light manipulation methods.
 */
public class LuaLightWrapper {

    public static LuaValue wrap(Light light, WorldClient world) {
        LuaTable metatable = new LuaTable();
        LuaTable index = new LuaTable();

        // -- Position --

        index.set("getPos", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable t = new LuaTable();
                t.set("x", LuaValue.valueOf(light.getPos().x));
                t.set("y", LuaValue.valueOf(light.getPos().y));
                t.set("z", LuaValue.valueOf(light.getPos().z));
                return t;
            }
        });

        index.set("setPos", new ThreeArgFunction() {
            @Override public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                light.pos((float) x.checkdouble(), (float) y.checkdouble(), (float) z.checkdouble());
                return LuaValue.NIL;
            }
        });

        // -- Direction --

        index.set("getDirection", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable t = new LuaTable();
                t.set("x", LuaValue.valueOf(light.getDirection().x));
                t.set("y", LuaValue.valueOf(light.getDirection().y));
                t.set("z", LuaValue.valueOf(light.getDirection().z));
                return t;
            }
        });

        index.set("setDirection", new ThreeArgFunction() {
            @Override public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                light.direction((float) x.checkdouble(), (float) y.checkdouble(), (float) z.checkdouble());
                return LuaValue.NIL;
            }
        });

        // -- Color --

        index.set("getColor", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(light.getColor());
            }
        });

        index.set("setColor", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                light.color(arg.checkint());
                return LuaValue.NIL;
            }
        });

        // -- Intensity --

        index.set("getIntensity", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(light.getIntensity());
            }
        });

        index.set("setIntensity", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                light.intensity((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });

        // -- Shadows --

        index.set("castsShadows", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(light.castsShadows());
            }
        });

        index.set("setCastsShadows", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                light.castsShadows(arg.checkboolean());
                return LuaValue.NIL;
            }
        });

        // -- Glare --

        index.set("getGlareSize", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(light.getGlareSize());
            }
        });

        index.set("setGlareSize", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                light.glareSize((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("getGlareIntensity", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(light.getGlareIntensity());
            }
        });

        index.set("setGlareIntensity", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                light.glareIntensity((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });

        // -- Type --

        index.set("getType", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(light.getType().name());
            }
        });

        // -- PointLight methods --

        if (light instanceof PointLight point) {
            index.set("getFalloffStart", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    return LuaValue.valueOf(point.getFalloffStart());
                }
            });

            index.set("getFalloffEnd", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    return LuaValue.valueOf(point.getFalloffEnd());
                }
            });

            index.set("setFalloff", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue start, LuaValue end) {
                    if (end.isnil()) {
                        point.falloff((float) start.checkdouble());
                    } else {
                        point.falloff((float) start.checkdouble(), (float) end.checkdouble());
                    }
                    return LuaValue.NIL;
                }
            });
        }

        // -- Spotlight methods --

        if (light instanceof Spotlight spot) {
            index.set("getInnerAngle", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    return LuaValue.valueOf(spot.getInnerAngle());
                }
            });

            index.set("getOuterAngle", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    return LuaValue.valueOf(spot.getOuterAngle());
                }
            });

            index.set("setAngle", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue inner, LuaValue outer) {
                    if (outer.isnil()) {
                        spot.angle((float) inner.checkdouble());
                    } else {
                        spot.angle((float) inner.checkdouble(), (float) outer.checkdouble());
                    }
                    return LuaValue.NIL;
                }
            });

            index.set("getBeamStrength", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    return LuaValue.valueOf(spot.getBeamStrength());
                }
            });

            index.set("setBeamStrength", new OneArgFunction() {
                @Override public LuaValue call(LuaValue arg) {
                    spot.beamStrength((float) arg.checkdouble());
                    return LuaValue.NIL;
                }
            });
        }

        // -- Remove --

        index.set("remove", new ZeroArgFunction() {
            @Override public LuaValue call() {
                world.removeLight(light);
                return LuaValue.NIL;
            }
        });

        // -- Metatable --

        metatable.set("__index", index);
        metatable.set("__tostring", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf("Light[" + light.getType().name() + "]");
            }
        });

        LuaUserdata userdata = new LuaUserdata(light);
        userdata.setmetatable(metatable);
        return userdata;
    }
}
