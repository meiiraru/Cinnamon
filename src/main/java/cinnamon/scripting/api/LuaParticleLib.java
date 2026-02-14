package cinnamon.scripting.api;

import cinnamon.text.Text;
import cinnamon.world.particle.*;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.joml.Vector3f;

/**
 * Lua "particles" library - spawn particles in the world.
 */
public class LuaParticleLib extends LuaTable {

    private final WorldClient world;

    public LuaParticleLib(WorldClient world) {
        this.world = world;

        // Spawn a particle by type
        set("spawn", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String typeName = args.checkjstring(1).toUpperCase();
                float x = (float) args.checkdouble(2);
                float y = (float) args.checkdouble(3);
                float z = (float) args.checkdouble(4);

                // Optional options table
                LuaTable opts = args.narg() >= 5 && args.arg(5).istable() ? args.checktable(5) : new LuaTable();
                int lifetime = opts.get("lifetime").isnil() ? 20 : opts.get("lifetime").checkint();
                int color = opts.get("color").isnil() ? 0xFFFFFFFF : opts.get("color").checkint();
                float mx = opts.get("motionX").isnil() ? 0f : (float) opts.get("motionX").checkdouble();
                float my = opts.get("motionY").isnil() ? 0f : (float) opts.get("motionY").checkdouble();
                float mz = opts.get("motionZ").isnil() ? 0f : (float) opts.get("motionZ").checkdouble();
                boolean emissive = !opts.get("emissive").isnil() && opts.get("emissive").checkboolean();

                Particle particle = createParticle(typeName, lifetime, color, opts);
                if (particle == null) {
                    return LuaValue.error("Unknown particle type: " + typeName);
                }

                particle.setPos(x, y, z);
                particle.setMotion(mx, my, mz);
                particle.setEmissive(emissive);

                world.addParticle(particle);
                return LuaValue.TRUE;
            }
        });

        // Spawn text particle
        set("spawnText", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String text = args.checkjstring(1);
                float x = (float) args.checkdouble(2);
                float y = (float) args.checkdouble(3);
                float z = (float) args.checkdouble(4);
                int lifetime = args.narg() >= 5 ? args.checkint(5) : 40;

                TextParticle particle = new TextParticle(Text.of(text), lifetime, new Vector3f(x, y, z));
                particle.setEmissive(true);
                world.addParticle(particle);
                return LuaValue.TRUE;
            }
        });

        // Spawn voxel particle
        set("spawnVoxel", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                float x = (float) args.checkdouble(1);
                float y = (float) args.checkdouble(2);
                float z = (float) args.checkdouble(3);
                int color = args.narg() >= 4 ? args.checkint(4) : 0xFFFF00FF;
                int lifetime = args.narg() >= 5 ? args.checkint(5) : 40;
                float size = args.narg() >= 6 ? (float) args.checkdouble(6) : 0.3f;

                VoxelParticle particle = new VoxelParticle(color, lifetime);
                particle.setPos(x, y, z);
                particle.setSize(size);
                particle.setEmissive(true);
                world.addParticle(particle);
                return LuaValue.TRUE;
            }
        });
    }

    private Particle createParticle(String typeName, int lifetime, int color, LuaTable opts) {
        return switch (typeName) {
            case "HEART", "HEARTH" -> {
                HeartParticle p = new HeartParticle(lifetime, color);
                yield p;
            }
            case "BROKEN_HEART", "BROKEN_HEARTH" -> new BrokenHeartParticle(lifetime, color);
            case "BUBBLE" -> new BubbleParticle(lifetime, color);
            case "CONFETTI" -> new ConfettiParticle(lifetime, color);
            case "DUST" -> new DustParticle(lifetime, color);
            case "ELECTRO" -> new ElectroParticle(lifetime, color);
            case "EXPLOSION" -> new ExplosionParticle(lifetime);
            case "FIRE" -> new FireParticle(lifetime);
            case "LIGHT" -> new LightParticle(lifetime, color);
            case "SMOKE" -> new SmokeParticle(lifetime, color);
            case "SQUARE" -> new SquareParticle(lifetime, color);
            case "STAR" -> new StarParticle(lifetime, color);
            case "STEAM" -> new SteamParticle(lifetime, color);
            case "VOXEL" -> {
                VoxelParticle p = new VoxelParticle(color, lifetime);
                float size = opts.get("size").isnil() ? 0.3f : (float) opts.get("size").checkdouble();
                p.setSize(size);
                yield p;
            }
            default -> null;
        };
    }
}
