package mayo.world.entity.projectile;

import mayo.render.Model;
import mayo.utils.Colors;
import mayo.utils.Meth;
import mayo.world.World;
import mayo.world.effects.Effect;
import mayo.world.entity.Entity;
import mayo.world.entity.PhysEntity;
import mayo.world.entity.living.LivingEntity;
import mayo.world.particle.DustParticle;
import org.joml.Vector3f;

public abstract class Projectile extends PhysEntity {

    protected final int damage;
    protected final float speed;
    protected final boolean crit;
    protected final Entity owner;
    protected int lifetime;

    public Projectile(Model model, World world, int damage, int lifetime, float speed, float critChance, Entity owner) {
        super(model, world);
        this.lifetime = lifetime;
        this.speed = speed;
        this.owner = owner;

        this.damage = calculateDamage(owner, damage);
        this.crit = checkCrit(owner, critChance);
    }

    private static int calculateDamage(Entity owner, int baseDamage) {
        if (owner instanceof LivingEntity le) {
            if (le.hasEffect(Effect.Type.PACIFIST))
                return 0;
            Effect boost = le.getEffect(Effect.Type.DAMAGE_BOOST);
            if (boost != null)
                return baseDamage + boost.getAmplitude();
        }

        return baseDamage;
    }

    private static boolean checkCrit(Entity owner, float chance) {
        if (owner instanceof LivingEntity le) {
            if (le.hasEffect(Effect.Type.ALWAYS_CRIT))
                return true;
            if (le.hasEffect(Effect.Type.NEVER_CRIT))
                return false;
        }

        return Math.random() < chance;
    }

    @Override
    public void onAdd() {
        super.onAdd();
        this.move(0, 0, 1);
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if (getDamage() == 0) confetti(world, pos);
    }

    @Override
    public void tick() {
        super.tick();
        lifetime--;

        if (lifetime <= 0)
            removed = true;
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);

        if (isRemoved() || entity == getOwner())
            return;

        if (entity instanceof LivingEntity le) {
            le.damage(this.owner, getDamage(), this.crit);
            removed = true;
        }
    }

    @Override
    protected float getMoveSpeed() {
        return speed;
    }

    public static void confetti(World world, Vector3f pos) {
        for (int i = 0; i < 20; i++) {
            DustParticle particle = new DustParticle((int) (Math.random() * 40) + 20, Colors.randomRainbow().rgba);
            particle.setPos(pos);
            particle.setMotion(Meth.rotToDir((float) Math.random() * 360, (float) Math.random() * 360).mul((float) Math.random() * 0.075f + 0.075f));
            particle.setScale(1.5f);
            world.addParticle(particle);
        }
    }

    public int getDamage() {
        return damage;
    }

    public Entity getOwner() {
        return owner;
    }
}
