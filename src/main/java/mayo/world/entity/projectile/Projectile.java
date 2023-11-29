package mayo.world.entity.projectile;

import mayo.render.Model;
import mayo.utils.Colors;
import mayo.utils.Maths;
import mayo.world.DamageType;
import mayo.world.World;
import mayo.world.effects.Effect;
import mayo.world.entity.Entity;
import mayo.world.entity.PhysEntity;
import mayo.world.entity.living.LivingEntity;
import mayo.world.particle.DustParticle;

public abstract class Projectile extends PhysEntity {

    protected final int damage;
    protected final float speed;
    protected final boolean crit;
    protected final Entity owner;
    protected int lifetime;

    public Projectile(Model model, int damage, int lifetime, float speed, float critChance, Entity owner) {
        super(model);
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
    public void tick() {
        super.tick();
        lifetime--;

        if (lifetime <= 0 && !isRemoved())
            remove();
    }

    @Override
    protected void applyMovement() {
        this.motion.add(move);
    }

    @Override
    public void onAdded(World world) {
        super.onAdded(world);
        this.move(0, 0, 1);
    }

    @Override
    public void remove() {
        super.remove();
        if (getDamage() == 0) confetti();
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);

        if (isRemoved() || entity == getOwner())
            return;

        if (entity.damage(this.owner, DamageType.PROJECTILE, getDamage(), this.crit))
            remove();
    }

    @Override
    protected float getMoveSpeed() {
        return speed;
    }

    protected void confetti() {
        for (int i = 0; i < 20; i++) {
            DustParticle particle = new DustParticle((int) (Math.random() * 40) + 20, Colors.randomRainbow().rgba);
            particle.setPos(pos);
            particle.setMotion(Maths.rotToDir((float) Math.random() * 360, (float) Math.random() * 360).mul((float) Math.random() * 0.075f + 0.075f));
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

    @Override
    public boolean isTargetable() {
        return false;
    }
}
