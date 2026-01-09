package cinnamon.world.entity.living;

import cinnamon.registry.EntityRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.world.Abilities;
import cinnamon.world.DamageType;
import cinnamon.world.entity.Entity;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class Player extends LivingEntity {

    private static final int MAX_HEALTH = 100;
    private static final int INVULNERABILITY_TIME = 10;
    private static final int INVENTORY_SIZE = 9;
    private static final float EYE_HEIGHT = 1.6f;
    private static final Vector3f DIMENSIONS = new Vector3f(0.6f, 1.8f, 0.6f);

    private final Abilities abilities = new Abilities();

    private int invulnerability = 0;
    private Entity damageSource;
    private int damageSourceTicks = 0;

    private boolean sprinting, sneaking, flying;

    public Player(String name, UUID uuid) {
        this(name, uuid, LivingModelRegistry.STRAWBERRY);
    }

    public Player(String name, UUID uuid, LivingModelRegistry model) {
        super(uuid, model.resource, model.eyeHeight, MAX_HEALTH, INVENTORY_SIZE);
        this.setName(name);
    }

    @Override
    public void tick() {
        super.tick();

        if (invulnerability > 0)
            invulnerability--;

        if (damageSourceTicks > 0)
            damageSourceTicks--;

        if (flying && (onGround || isRiding()))
            flying = false;
    }

    @Override
    protected void applyForces() {
        if (!flying) super.applyForces();
    }

    @Override
    protected void applyImpulse() {
        if (flying) {
            this.motion.add(impulse);
            this.impulse.set(0);
        } else {
            super.applyImpulse();
        }
    }

    @Override
    protected void motionFallout() {
        if (flying) {
            this.motion.mul(0.6f);
        } else {
            super.motionFallout();
        }
    }

    @Override
    protected Vector3f tickTerrainCollisions(AABB aabb) {
        return abilities.noclip() ? new Vector3f(motion) : super.tickTerrainCollisions(aabb);
    }

    @Override
    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        if (!abilities.noclip())
            super.tickEntityCollisions(aabb, toMove);
    }

    @Override
    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        if (invulnerability > 0 || (getAbilities().godMode() && type != DamageType.GOD))
            return false;

        this.invulnerability = INVULNERABILITY_TIME;
        boolean result = super.damage(source, type, amount, crit);

        if (result) {
            this.damageSource = source;
            this.damageSourceTicks = 30;
        }

        return result;
    }

    public Float getDamageAngle() {
        if (damageSource == null)
            return null;

        Vector3f diff = damageSource.getPos().sub(pos, new Vector3f()).normalize();
        return Maths.dirToRot(diff).y - rot.y;
    }

    public int getDamageSourceTicks() {
        return damageSourceTicks;
    }

    public void updateMovementFlags(boolean sneaking, boolean sprinting, boolean flying) {
        this.sneaking = sneaking;
        this.sprinting = sprinting;
        this.flying = (flying && abilities.canFly()) || abilities.noclip();

        if (this.isRiding() && sneaking)
            this.stopRiding();
    }

    @Override
    public void impulse(float left, float up, float forwards) {
        super.impulse(left, up, forwards);

        if (flying)
            impulse.y = Math.signum(up) * 0.15f;
    }

    @Override
    protected float getMoveSpeed() {
        float speed = super.getMoveSpeed();

        if (sneaking)
            speed *= getSneakingMultiplier();
        if (sprinting)
            speed *= flying ? getFlyingSprintMultiplier() : getSprintMultiplier();

        return speed;
    }

    protected float getSneakingMultiplier() {
        return 0.5f;
    }

    protected float getFlyingSprintMultiplier() {
        return 2.3f;
    }

    protected float getSprintMultiplier() {
        return 1.3f;
    }

    protected float clampPitch(float pitch) {
        return Math.max(Math.min(pitch, 90), -90);
    }

    @Override
    public void rotateTo(float pitch, float yaw) {
        super.rotateTo(clampPitch(pitch), yaw);
    }

    @Override
    public void setRot(float pitch, float yaw) {
        super.setRot(clampPitch(pitch), yaw);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.PLAYER;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isFlying() {
        return flying;
    }

    public Abilities getAbilities() {
        return abilities;
    }

    @Override
    protected void updateAABB() {
        aabb.set(getPos());
        float w = Math.max(DIMENSIONS.x, DIMENSIONS.z) * 0.5f;
        float y = model.getAABB().getHeight(); //Math.min(, DIMENSIONS.y);
        aabb.inflate(w, 0, w, w, y, w);
    }
}
