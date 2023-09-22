package mayo.world.entity.projectile;

import mayo.model.obj.Mesh;
import mayo.parsers.ObjLoader;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Bullet extends Projectile {

    private static final Mesh MODEL = ObjLoader.load(new Resource("models/entities/bullet/bullet.obj")).bake();
    private static final int DAMAGE = 5;
    private static final int LIFETIME = 30;
    private static final float SPEED = 0.5f;

    public Bullet(World world, Entity owner) {
        super(MODEL, world, MODEL.getBoundingBox(), DAMAGE, LIFETIME, SPEED);
        this.setOwner(owner);
    }
}
