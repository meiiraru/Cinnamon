package cinnamon.world.entity.living;

import cinnamon.registry.EntityRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.world.DamageType;
import cinnamon.world.ai.AIBehaviour;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.PhysEntity;
import org.joml.Vector3f;

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
        super(uuid, entityModel.resource, entityModel.eyeHeight, MAX_HEALTH, INVENTORY_SIZE);
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
    protected void collide(PhysEntity entity, CollisionResult result, Vector3f toMove) {
        super.collide(entity, result, toMove);
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
