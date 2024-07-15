package cinnamon.world.entity.collectable;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class EffectBox extends Collectable {

    private static final List<Supplier<Effect>> EFFECT_LIST = List.of(
            () -> Effect.Type.NEVER_CRIT.create(200),
            () -> Effect.Type.ALWAYS_CRIT.create(300),
            () -> Effect.Type.PACIFIST.create(100),
            () -> Effect.Type.DAMAGE_BOOST.create(150, 2),
            () -> Effect.Type.DAMAGE_BOOST.create(200, 1),
            () -> Effect.Type.HEAL.create(200, 2),
            () -> Effect.Type.SPEED.create(300),
            () -> Effect.Type.EXPLOSION_IMMUNITY.create(200)
    );

    public EffectBox(UUID uuid) {
        super(uuid, EntityModelRegistry.EFFECT_BOX.model);
    }

    @Override
    protected boolean onPickUp(Entity entity) {
        if (entity instanceof Player p) {
            p.giveEffect(EFFECT_LIST.get((int) (Math.random() * EFFECT_LIST.size())).get());
            return true;
        }

        return false;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.EFFECT_BOX;
    }
}
