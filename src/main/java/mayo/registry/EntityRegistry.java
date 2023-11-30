package mayo.registry;

import com.esotericsoftware.kryo.Kryo;
import mayo.world.entity.Entity;
import mayo.world.entity.collectable.EffectBox;
import mayo.world.entity.collectable.HealthPack;
import mayo.world.entity.living.Enemy;
import mayo.world.entity.living.Player;
import mayo.world.entity.projectile.*;
import mayo.world.entity.vehicle.*;

import java.util.function.Supplier;

public enum EntityRegistry implements Registry {
    //collectable
    EFFECT_BOX(EffectBox.class, EffectBox::new),
    HEALTH_PACK(HealthPack.class, HealthPack::new),

    //living
    ENEMY(Enemy.class, Enemy::new),
    PLAYER(Player.class, Player::new), //todo - NO - NO - NO ---- WRONG

    //projectiles
    CANDY(Candy.class, () -> new Candy(null)),
    POTATO(Potato.class, () -> new Potato(null)),
    RICE(Rice.class, () -> new Rice(null)),
    RICE_BALL(RiceBall.class, () -> new RiceBall(null)),

    //vehicles
    CART(Cart.class, Cart::new);

    private final Class<? extends Entity> clazz;
    private final Supplier<Entity> factory;

    EntityRegistry(Class<? extends Entity> clazz, Supplier<Entity> factory) {
        this.clazz = clazz;
        this.factory = factory;
    }

    @Override
    public void register(Kryo kryo) {
        kryo.register(clazz);
    }

    public Supplier<Entity> getFactory() {
        return factory;
    }
}
