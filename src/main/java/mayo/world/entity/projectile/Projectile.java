package mayo.world.entity.projectile;

import mayo.render.Model;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.living.LivingEntity;
import org.joml.Vector3f;

public abstract class Projectile extends Entity {

    private final int damage;
    private final float speed;
    private final boolean crit;
    private int lifetime;
    private Entity owner;

    public Projectile(Model model, World world, Vector3f dimensions, int damage, int lifetime, float speed, boolean crit) {
        super(model, world, dimensions);
        this.damage = damage;
        this.lifetime = lifetime;
        this.speed = speed;
        this.crit = crit;
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
            le.damage(getDamage(), this.crit);
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
