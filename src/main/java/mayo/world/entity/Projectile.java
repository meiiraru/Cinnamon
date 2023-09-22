package mayo.world.entity;

import mayo.model.obj.Mesh;
import mayo.world.World;
import org.joml.Vector3f;

public abstract class Projectile extends Entity {

    private final int damage;
    private Entity owner;

    public Projectile(Mesh model, World world, Vector3f dimensions, int damage) {
        super(model, world, dimensions);
        this.damage = damage;
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);
        if (entity instanceof LivingEntity le)
            le.damage(getDamage());
    }

    public int getDamage() {
        return damage;
    }

    public Entity getOwner() {
        return owner;
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
    }
}
