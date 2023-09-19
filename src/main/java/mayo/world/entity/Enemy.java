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

    public Enemy(World world) {
        super(ENEMY_MODEL, world, DIMENSIONS, MAX_HEALTH);
    }
}
