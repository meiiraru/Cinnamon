package cinnamon.scripting.wrappers;

import cinnamon.world.DamageType;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.living.LivingEntity;
import org.joml.Vector3f;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Wraps a Java Entity as a LuaUserdata with a metatable exposing all entity methods.
 * Handles instanceof checks to expose PhysEntity/LivingEntity methods when applicable.
 */
public class LuaEntityWrapper {

    public static LuaValue wrap(Entity entity) {
        LuaTable metatable = new LuaTable();
        LuaTable index = new LuaTable();

        // -- Base Entity methods --

        index.set("getUUID", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(entity.getUUID().toString());
            }
        });

        index.set("getType", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(entity.getType().name());
            }
        });

        index.set("getPos", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable t = new LuaTable();
                t.set("x", LuaValue.valueOf(entity.getPos().x));
                t.set("y", LuaValue.valueOf(entity.getPos().y));
                t.set("z", LuaValue.valueOf(entity.getPos().z));
                return t;
            }
        });

        index.set("setPos", new ThreeArgFunction() {
            @Override public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                entity.setPos((float) x.checkdouble(), (float) y.checkdouble(), (float) z.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("moveTo", new ThreeArgFunction() {
            @Override public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                entity.moveTo((float) x.checkdouble(), (float) y.checkdouble(), (float) z.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("getRot", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable t = new LuaTable();
                t.set("pitch", LuaValue.valueOf(entity.getRot().x));
                t.set("yaw", LuaValue.valueOf(entity.getRot().y));
                return t;
            }
        });

        index.set("setRot", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue pitch, LuaValue yaw) {
                entity.setRot((float) pitch.checkdouble(), (float) yaw.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("lookAt", new ThreeArgFunction() {
            @Override public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                entity.lookAt((float) x.checkdouble(), (float) y.checkdouble(), (float) z.checkdouble());
                return LuaValue.NIL;
            }
        });

        index.set("remove", new ZeroArgFunction() {
            @Override public LuaValue call() {
                entity.remove();
                return LuaValue.NIL;
            }
        });

        index.set("isRemoved", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(entity.isRemoved());
            }
        });

        index.set("getName", new ZeroArgFunction() {
            @Override public LuaValue call() {
                String name = entity.getName();
                return name != null ? LuaValue.valueOf(name) : LuaValue.NIL;
            }
        });

        index.set("setName", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                entity.setName(arg.checkjstring());
                return LuaValue.NIL;
            }
        });

        index.set("isVisible", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(entity.isVisible());
            }
        });

        index.set("setVisible", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                entity.setVisible(arg.checkboolean());
                return LuaValue.NIL;
            }
        });

        index.set("isSilent", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(entity.isSilent());
            }
        });

        index.set("setSilent", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                entity.setSilent(arg.checkboolean());
                return LuaValue.NIL;
            }
        });

        index.set("getLookDir", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Vector3f dir = entity.getLookDir();
                LuaTable t = new LuaTable();
                t.set("x", LuaValue.valueOf(dir.x));
                t.set("y", LuaValue.valueOf(dir.y));
                t.set("z", LuaValue.valueOf(dir.z));
                return t;
            }
        });

        // -- PhysEntity methods --

        if (entity instanceof PhysEntity phys) {
            index.set("setMotion", new ThreeArgFunction() {
                @Override public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                    phys.setMotion((float) x.checkdouble(), (float) y.checkdouble(), (float) z.checkdouble());
                    return LuaValue.NIL;
                }
            });

            index.set("getMotion", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    Vector3f m = phys.getMotion();
                    LuaTable t = new LuaTable();
                    t.set("x", LuaValue.valueOf(m.x));
                    t.set("y", LuaValue.valueOf(m.y));
                    t.set("z", LuaValue.valueOf(m.z));
                    return t;
                }
            });

            index.set("impulse", new ThreeArgFunction() {
                @Override public LuaValue call(LuaValue left, LuaValue up, LuaValue fwd) {
                    phys.impulse((float) left.checkdouble(), (float) up.checkdouble(), (float) fwd.checkdouble());
                    return LuaValue.NIL;
                }
            });

            index.set("knockback", new VarArgFunction() {
                @Override public Varargs invoke(Varargs args) {
                    float dx = (float) args.checkdouble(1);
                    float dy = (float) args.checkdouble(2);
                    float dz = (float) args.checkdouble(3);
                    float force = (float) args.checkdouble(4);
                    phys.knockback(new Vector3f(dx, dy, dz), force);
                    return LuaValue.NIL;
                }
            });

            index.set("setGravity", new OneArgFunction() {
                @Override public LuaValue call(LuaValue arg) {
                    phys.setGravity((float) arg.checkdouble());
                    return LuaValue.NIL;
                }
            });

            index.set("getGravity", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    return LuaValue.valueOf(phys.getGravity());
                }
            });
        }

        // -- LivingEntity methods --

        if (entity instanceof LivingEntity living) {
            index.set("getHealth", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    return LuaValue.valueOf(living.getHealth());
                }
            });

            index.set("setHealth", new OneArgFunction() {
                @Override public LuaValue call(LuaValue arg) {
                    living.setHealth(arg.checkint());
                    return LuaValue.NIL;
                }
            });

            index.set("heal", new OneArgFunction() {
                @Override public LuaValue call(LuaValue arg) {
                    living.heal(arg.checkint());
                    return LuaValue.NIL;
                }
            });

            index.set("kill", new ZeroArgFunction() {
                @Override public LuaValue call() {
                    living.kill();
                    return LuaValue.NIL;
                }
            });

            index.set("damage", new VarArgFunction() {
                @Override public Varargs invoke(Varargs args) {
                    String damageTypeName = args.narg() >= 1 ? args.checkjstring(1).toUpperCase() : "GOD";
                    int amount = args.narg() >= 2 ? args.checkint(2) : 1;
                    boolean crit = args.narg() >= 3 && args.checkboolean(3);
                    DamageType type;
                    try {
                        type = DamageType.valueOf(damageTypeName);
                    } catch (IllegalArgumentException e) {
                        type = DamageType.GOD;
                    }
                    living.damage(null, type, amount, crit);
                    return LuaValue.NIL;
                }
            });
        }

        // -- Metatable setup --

        metatable.set("__index", index);
        metatable.set("__tostring", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf("Entity[" + entity.getType().name() + ":" + entity.getUUID().toString().substring(0, 8) + "]");
            }
        });

        LuaUserdata userdata = new LuaUserdata(entity);
        userdata.setmetatable(metatable);
        return userdata;
    }
}
