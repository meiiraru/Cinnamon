package cinnamon.registry;

import com.esotericsoftware.kryo.Kryo;
import cinnamon.world.WorldClient;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.collectable.EffectBox;
import cinnamon.world.entity.collectable.HealthPack;
import cinnamon.world.entity.living.Dummy;
import cinnamon.world.entity.living.Enemy;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.projectile.Candy;
import cinnamon.world.entity.projectile.Potato;
import cinnamon.world.entity.projectile.Rice;
import cinnamon.world.entity.projectile.RiceBall;
import cinnamon.world.entity.vehicle.Cart;

import java.util.UUID;
import java.util.function.Function;

public enum EntityRegistry implements Registry {
    //collectable
    EFFECT_BOX(EffectBox.class, EffectBox::new),
    HEALTH_PACK(HealthPack.class, HealthPack::new),

    //living
    ENEMY(Enemy.class, Enemy::new),
    PLAYER(Player.class, uuid -> new Player("", uuid)),
    DUMMY(Dummy.class, Dummy::new),

    //projectiles
    CANDY(Candy.class, uuid -> new Candy(uuid, null)),
    POTATO(Potato.class, uuid -> new Potato(uuid, null)),
    RICE(Rice.class, uuid -> new Rice(uuid, null)),
    RICE_BALL(RiceBall.class, uuid -> new RiceBall(uuid, null)),

    //vehicles
    CART(Cart.class, Cart::new);

    private final Class<? extends Entity> clazz;
    private final Function<UUID, Entity> factory;

    EntityRegistry(Class<? extends Entity> clazz, Function<UUID, Entity> factory) {
        this.clazz = clazz;
        this.factory = factory;
    }

    @Override
    public void register(Kryo kryo) {
        kryo.register(clazz);
    }

    public Function<UUID, Entity> getFactory() {
        return factory;
    }
}
