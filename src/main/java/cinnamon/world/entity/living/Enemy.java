package cinnamon.world.entity.living;

import cinnamon.registry.EntityRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.world.AIBehaviour;
import cinnamon.world.DamageType;
import cinnamon.world.entity.Entity;

import java.util.UUID;

public class Enemy extends LivingEntity {

    private static final int MAX_HEALTH = 20;
    private static final int MELEE_DAMAGE = 5;
    private static final int INVENTORY_SIZE = 1;
    private final AIBehaviour[] behaviours;

    public Enemy(UUID uuid, AIBehaviour... behaviours) {
        this(uuid, LivingModelRegistry.random(), behaviours);
    }

    private Enemy(UUID uuid, LivingModelRegistry entityModel, AIBehaviour... behaviours) {
        super(uuid, entityModel, MAX_HEALTH, INVENTORY_SIZE);
        this.behaviours = behaviours;
    }

    @Override
    public void tick() {
        super.tick();

        for (AIBehaviour behaviour : behaviours) {
            if (behaviour != null)
                behaviour.apply(this);
        }
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);
        if (entity instanceof Player p)
            p.damage(this, DamageType.MELEE, MELEE_DAMAGE, false);
    }

    @Override
    protected float getMoveSpeed() {
        return super.getMoveSpeed() * 0.5f;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.ENEMY;
    }
}
