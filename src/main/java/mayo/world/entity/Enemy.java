package mayo.world.entity;

import mayo.model.LivingEntityModels;
import mayo.world.World;

public class Enemy extends LivingEntity {

    private static final int MAX_HEALTH = 20;
    private static final int MELEE_DAMAGE = 5;

    public Enemy(World world) {
        this(world, LivingEntityModels.random());
    }

    private Enemy(World world, LivingEntityModels entityModel) {
        super(entityModel, world, MAX_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();

        //todo - lol
        this.move(0, 0, 0.1f);
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);
        if (entity instanceof Player p)
            p.damage(MELEE_DAMAGE);
    }
}
