package mayo.world.entity.living;

import mayo.registry.LivingModelRegistry;
import mayo.world.AIBehaviour;
import mayo.world.DamageType;
import mayo.world.entity.Entity;

public class Enemy extends LivingEntity {

    private static final int MAX_HEALTH = 20;
    private static final int MELEE_DAMAGE = 5;
    private static final int INVENTORY_SIZE = 1;
    private final AIBehaviour[] behaviours;

    public Enemy(AIBehaviour... behaviours) {
        this(LivingModelRegistry.random(), behaviours);
    }

    private Enemy(LivingModelRegistry entityModel, AIBehaviour... behaviours) {
        super(entityModel, MAX_HEALTH, INVENTORY_SIZE);
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
}
