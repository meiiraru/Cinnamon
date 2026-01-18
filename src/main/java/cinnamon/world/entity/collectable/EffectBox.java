package cinnamon.world.entity.collectable;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.world.WorldClient;
import org.joml.Math;

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
            () -> Effect.Type.EXPLOSION_IMMUNITY.create(200),
            () -> Effect.Type.GLOWING.create(600)
    );
    private static final Resource[] PICKUP_SOUND = {
            new Resource("sounds/entity/effect/1.ogg"),
            new Resource("sounds/entity/effect/2.ogg"),
            new Resource("sounds/entity/effect/3.ogg")
    };

    public EffectBox(UUID uuid) {
        super(uuid, EntityModelRegistry.EFFECT_BOX.resource);
    }

    @Override
    protected boolean onPickUp(Entity entity) {
        if (entity instanceof LivingEntity le) {
            int index = (int) (Math.random() * PICKUP_SOUND.length);
            Resource sound = PICKUP_SOUND[index];
            if (!isSilent() && le.getWorld().isClientside())
                ((WorldClient) le.getWorld()).playSound(sound, SoundCategory.ENTITY, le.getPos()).pitch(Maths.range(0.95f, 1.05f));
            le.giveEffect(EFFECT_LIST.get((int) (Math.random() * EFFECT_LIST.size())).get());
            return true;
        }

        return false;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.EFFECT_BOX;
    }
}
