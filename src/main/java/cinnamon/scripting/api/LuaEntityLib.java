package cinnamon.scripting.api;

import cinnamon.registry.EntityRegistry;
import cinnamon.scripting.wrappers.LuaEntityWrapper;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.collectable.EffectBox;
import cinnamon.world.entity.collectable.HealthPack;
import cinnamon.world.entity.collectable.ItemEntity;
import cinnamon.world.entity.living.Dummy;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.misc.Firework;
import cinnamon.world.entity.misc.Spawner;
import cinnamon.world.entity.misc.TriggerArea;
import cinnamon.world.entity.vehicle.Cart;
import cinnamon.world.entity.vehicle.ShoppingCart;
import cinnamon.world.items.PotatoItem;
import cinnamon.world.world.WorldClient;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import java.util.UUID;

/**
 * Lua "entity" library - spawn and manage entities.
 */
public class LuaEntityLib extends LuaTable {

    private final WorldClient world;

    public LuaEntityLib(WorldClient world) {
        this.world = world;

        // Spawn entity by type name
        set("spawn", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String typeName = args.checkjstring(1).toUpperCase();
                float x = (float) args.checkdouble(2);
                float y = (float) args.checkdouble(3);
                float z = (float) args.checkdouble(4);

                Entity entity = createEntity(typeName);
                if (entity == null) {
                    return LuaValue.error("Unknown entity type: " + typeName);
                }

                entity.setPos(x, y, z);
                world.addEntity(entity);
                return LuaEntityWrapper.wrap(entity);
            }
        });

        // Spawn a spawner
        set("spawnSpawner", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                String typeName = args.checkjstring(1).toUpperCase();
                float x = (float) args.checkdouble(2);
                float y = (float) args.checkdouble(3);
                float z = (float) args.checkdouble(4);
                float radius = args.narg() >= 5 ? (float) args.checkdouble(5) : 0f;
                int delay = args.narg() >= 6 ? args.checkint(6) : 100;

                Spawner<?> spawner = createSpawner(typeName, radius, delay);
                if (spawner == null) {
                    return LuaValue.error("Cannot create spawner for type: " + typeName);
                }

                spawner.setPos(x, y, z);
                spawner.setRenderCooldown(true);
                world.addEntity(spawner);
                return LuaEntityWrapper.wrap(spawner);
            }
        });

        // Spawn trigger area
        set("spawnTriggerArea", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                float x = (float) args.checkdouble(1);
                float y = (float) args.checkdouble(2);
                float z = (float) args.checkdouble(3);
                float sx = (float) args.checkdouble(4);
                float sy = (float) args.checkdouble(5);
                float sz = (float) args.checkdouble(6);

                TriggerArea trigger = new TriggerArea(UUID.randomUUID(), sx, sy, sz);
                trigger.setPos(x, y, z);

                // Optional callbacks - onEnter(7), onStay(8), onExit(9)
                if (args.narg() >= 7 && args.arg(7).isfunction()) {
                    LuaValue onEnter = args.arg(7);
                    trigger.setEnterTrigger(e -> onEnter.call(LuaEntityWrapper.wrap(e)));
                }
                if (args.narg() >= 8 && args.arg(8).isfunction()) {
                    LuaValue onStay = args.arg(8);
                    trigger.setStayTrigger(e -> onStay.call(LuaEntityWrapper.wrap(e)));
                }
                if (args.narg() >= 9 && args.arg(9).isfunction()) {
                    LuaValue onExit = args.arg(9);
                    trigger.setExitTrigger(e -> onExit.call(LuaEntityWrapper.wrap(e)));
                }

                world.addEntity(trigger);
                return LuaEntityWrapper.wrap(trigger);
            }
        });

        // Get entity by UUID
        set("get", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Entity e = world.getEntityByUUID(UUID.fromString(arg.checkjstring()));
                return e != null ? LuaEntityWrapper.wrap(e) : LuaValue.NIL;
            }
        });

        // List all entity type names
        set("types", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable table = new LuaTable();
                EntityRegistry[] values = EntityRegistry.values();
                for (int i = 0; i < values.length; i++) {
                    table.set(i + 1, LuaValue.valueOf(values[i].name()));
                }
                return table;
            }
        });
    }

    private Entity createEntity(String typeName) {
        return switch (typeName) {
            case "DUMMY" -> new Dummy(UUID.randomUUID());
            case "CART" -> new Cart(UUID.randomUUID());
            case "SHOPPING_CART" -> new ShoppingCart(UUID.randomUUID());
            case "HEALTH_PACK" -> new HealthPack(UUID.randomUUID());
            case "EFFECT_BOX" -> new EffectBox(UUID.randomUUID());
            case "ITEM" -> {
                ItemEntity item = new ItemEntity(UUID.randomUUID(), new PotatoItem(1));
                item.setPickUpDelay(0);
                yield item;
            }
            default -> null;
        };
    }

    private Spawner<?> createSpawner(String typeName, float radius, int delay) {
        return switch (typeName) {
            case "HEALTH_PACK" -> new Spawner<>(UUID.randomUUID(), radius, delay, () -> new HealthPack(UUID.randomUUID()));
            case "EFFECT_BOX" -> new Spawner<>(UUID.randomUUID(), radius, delay, () -> new EffectBox(UUID.randomUUID()));
            case "DUMMY" -> new Spawner<>(UUID.randomUUID(), radius, delay, () -> new Dummy(UUID.randomUUID()));
            case "ITEM" -> new Spawner<>(UUID.randomUUID(), radius, delay, () -> {
                ItemEntity e = new ItemEntity(UUID.randomUUID(), new PotatoItem(1));
                e.setPickUpDelay(0);
                return e;
            });
            default -> null;
        };
    }
}
