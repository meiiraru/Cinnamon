package mayo.world.entity.projectile;

import mayo.model.obj.Mesh;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.living.LivingEntity;
import org.joml.Vector3f;

public abstract class Projectile extends Entity {

    private final int damage;
    private final float speed;
    private int lifetime;
    private Entity owner;

    public Projectile(Mesh model, World world, Vector3f dimensions, int damage, int lifetime, float speed) {
        super(model, world, dimensions);
        this.damage = damage;
        this.lifetime = lifetime;
        this.speed = speed;
    }

    @Override
    public void tick() {
        super.tick();
        lifetime--;

        this.move(0, 0, speed);

        if (lifetime <= 0)
            removed = true;
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);

        if (isRemoved() || entity == getOwner())
            return;

        if (entity instanceof LivingEntity le) {
            le.damage(getDamage());
            removed = true;
        }
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
