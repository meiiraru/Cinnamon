package cinnamon.world.entity.projectile;

import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.DamageType;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.particle.DustParticle;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class Projectile extends PhysEntity {

    protected UUID owner;
    protected final float speed;
    protected final float critChance;
    protected int damage;
    protected boolean crit;

    protected int lifetime;

    public Projectile(UUID uuid, Resource model, int damage, int lifetime, float speed, float critChance, UUID owner) {
        super(uuid, model);
        this.lifetime = lifetime;
        this.speed = speed;
        this.damage = damage;
        this.critChance = critChance;
        this.owner = owner;
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
    protected void applyImpulse() {
        this.motion.add(impulse);
        this.impulse.set(0);
    }

    @Override
    public void onAdded(World world) {
        super.onAdded(world);

        //calculate damage - only on server
        //if (!world.isClientside()) {
        Entity owner = world.getEntityByUUID(this.owner);
        this.damage = calculateDamage(owner, damage);
        this.crit = checkCrit(owner, critChance);
        //}
    }

    @Override
    public void remove() {
        super.remove();
        if (getDamage() == 0 && getWorld().isClientside()) confetti();
    }

    @Override
    protected void collide(Entity entity, CollisionResult result, Vector3f toMove) {
        super.collide(entity, result, toMove);

        if (isRemoved() || entity.getUUID().equals(getOwner()))
            return;

        if (entity.damage(getWorld().getEntityByUUID(getOwner()), DamageType.PROJECTILE, getDamage(), this.crit))
            remove();
    }

    @Override
    protected float getMoveSpeed() {
        return speed;
    }

    protected void confetti() {
        WorldClient wc = (WorldClient) getWorld();
        for (int i = 0; i < 20; i++) {
            DustParticle particle = new DustParticle((int) (Math.random() * 40) + 20, Colors.randomRainbow().argb);
            particle.setPos(pos);
            particle.setMotion(Maths.rotToDir((float) Math.random() * 360, (float) Math.random() * 360).mul((float) Math.random() * 0.075f + 0.075f));
            particle.setScale(1.5f);
            wc.addParticle(particle);
        }
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public boolean isCrit() {
        return crit;
    }

    public void setCrit(boolean crit) {
        this.crit = crit;
    }

    public int getLifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID uuid) {
        this.owner = uuid;
    }

    @Override
    public boolean isTargetable() {
        return false;
    }
}
