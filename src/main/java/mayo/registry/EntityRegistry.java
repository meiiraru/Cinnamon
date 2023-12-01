package mayo.registry;

import com.esotericsoftware.kryo.Kryo;
import mayo.world.WorldClient;
import mayo.world.entity.Entity;
import mayo.world.entity.collectable.EffectBox;
import mayo.world.entity.collectable.HealthPack;
import mayo.world.entity.living.Enemy;
import mayo.world.entity.living.Player;
import mayo.world.entity.projectile.Candy;
import mayo.world.entity.projectile.Potato;
import mayo.world.entity.projectile.Rice;
import mayo.world.entity.projectile.RiceBall;
import mayo.world.entity.vehicle.Cart;

import java.util.UUID;
import java.util.function.Function;

public enum EntityRegistry implements Registry {
    //collectable
    EFFECT_BOX(EffectBox.class, EffectBox::new),
    HEALTH_PACK(HealthPack.class, HealthPack::new),

    //living
    ENEMY(Enemy.class, Enemy::new),
    PLAYER(Player.class, uuid -> {
        Player p = new Player(uuid);
        WorldClient.givePlayerItems(p); //TODO
        return p;
    }),

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
