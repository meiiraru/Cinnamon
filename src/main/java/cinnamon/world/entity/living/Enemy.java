package cinnamon.world.entity.living;

import cinnamon.math.collision.Hit;
import cinnamon.registry.EntityRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.world.ai.AIBehaviour;
import cinnamon.world.entity.DamageType;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

import java.util.UUID;

public class Enemy extends LivingEntity {

    public static final int MAX_HEALTH = 20;
    public static final int MELEE_DAMAGE = 5;
    protected final AIBehaviour[] behaviours;

    protected Entity target;

    public Enemy(UUID uuid, int inventorySize, AIBehaviour... behaviours) {
        this(uuid, LivingModelRegistry.random(), inventorySize, behaviours);
    }

    private Enemy(UUID uuid, LivingModelRegistry entityModel, int inventorySize, AIBehaviour... behaviours) {
        super(uuid, entityModel.resource, entityModel.eyeHeight, MAX_HEALTH, inventorySize);
        this.behaviours = behaviours;
    }

    @Override
    public void tick() {
        super.tick();

        Entity target = getTarget();
        if (target == null || target.isRemoved()) {
            setTarget(((WorldClient) getWorld()).playerEntity);
            return;
        }

        for (AIBehaviour behaviour : behaviours) {
            if (behaviour != null)
                behaviour.apply(this, target);
        }
    }

    @Override
    protected void collideEntity(PhysEntity entity, Hit result, Vector3f toMove) {
        super.collideEntity(entity, result, toMove);
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

    public void setTarget(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
