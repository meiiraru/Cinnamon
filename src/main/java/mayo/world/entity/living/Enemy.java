package mayo.world.entity.living;

import mayo.model.LivingEntityModels;
import mayo.world.World;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Enemy extends LivingEntity {

    private static final int MAX_HEALTH = 20;
    private static final int MELEE_DAMAGE = 5;
    private static final int INVENTORY_SIZE = 1;

    public Enemy(World world) {
        this(world, LivingEntityModels.random());
    }

    private Enemy(World world, LivingEntityModels entityModel) {
        super(entityModel, world, MAX_HEALTH, INVENTORY_SIZE);
    }

    @Override
    public void tick() {
        super.tick();

        //todo - lol
        this.move(0, 0, 0.1f);

        Vector3f pos = getWorld().player.getEyePos();
        this.lookAt(pos.x, pos.y, pos.z);
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);
        if (entity instanceof Player p)
            p.damage(MELEE_DAMAGE, false);
    }
}
