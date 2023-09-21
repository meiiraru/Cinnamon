package mayo.world.entity;

import mayo.model.ModelManager;
import mayo.model.obj.Mesh;
import mayo.utils.Resource;
import mayo.world.World;
import org.joml.Vector3f;

public class Enemy extends LivingEntity {

    private static final Mesh ENEMY_MODEL = ModelManager.load(new Resource("models/enemy/enemy.obj"));
    private static final Vector3f DIMENSIONS = ENEMY_MODEL.getBBMax().sub(ENEMY_MODEL.getBBMin(), new Vector3f());
    private static final int MAX_HEALTH = 20;
    private static final int MELEE_DAMAGE = 20;

    public Enemy(World world) {
        super(ENEMY_MODEL, world, DIMENSIONS, MAX_HEALTH);
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
