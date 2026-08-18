package cinnamon.world.entity.living;

import cinnamon.math.Maths;
import cinnamon.math.collision.shape.AABB;
import cinnamon.registry.EntityRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.settings.Settings;
import cinnamon.world.Abilities;
import cinnamon.world.entity.DamageType;
import cinnamon.world.entity.Entity;
import cinnamon.world.particle.SmokeParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class Player extends LivingEntity {

    private static final int MAX_HEALTH = 100;
    private static final int INVULNERABILITY_TIME = 10;
    private static final int INVENTORY_SIZE = 9;
    private static final int SPRINT_PARTICLE_DELAY = 3;
    private static final float EYE_HEIGHT = 1.6f;
    private static final Vector3f DIMENSIONS = new Vector3f(0.6f, 1.8f, 0.6f);

    private final Abilities abilities = new Abilities();

    private int invulnerability = 0;
    private Entity damageSource;
    private int damageSourceTicks = 0;
    private int sprintParticle = 0;

    private boolean sprinting, sneaking, flying;
    private boolean jumping, forwards;
    private int flyKeyTicks = 0;

    public Player(String name, UUID uuid) {
        this(name, uuid, LivingModelRegistry.STRAWBERRY);
    }

    public Player(String name, UUID uuid, LivingModelRegistry model) {
        super(uuid, model.resource, model.eyeHeight, MAX_HEALTH, INVENTORY_SIZE);
        this.setName(name);
        this.getController().bindState(
                "fly_toggle", Settings.jump.get(),
                jumping -> {
                    if (!this.jumping && Settings.jump.get().click()) {
                        if (flyKeyTicks > 0) {
                            updateMovementFlags(this.sneaking, this.sprinting, !this.flying);
                            flyKeyTicks = 0;
                        } else {
                            flyKeyTicks = Settings.doubleKeypressTime.get();
                        }
                    }
                    this.jumping = Settings.jump.get().isActuallyPressed();
                }
        ).bindState(
                "sneak", Settings.sneak.get(),
                sneaking -> updateMovementFlags(sneaking, this.sprinting, this.flying)
        ).bindState(
                "sprint", Settings.sprint.get(),
                sprinting -> {
                    forwards = Settings.forward.get().isPressed() && !Settings.backward.get().isPressed();
                    updateMovementFlags(this.sneaking, sprinting, this.flying);
                }
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (invulnerability > 0)
            invulnerability--;

        if (damageSourceTicks > 0)
            damageSourceTicks--;

        if (flyKeyTicks > 0)
            flyKeyTicks--;

        if (flying && (onGround || isRiding()))
            flying = false;

        if (this.isSprinting() && onGround && --sprintParticle <= 0) {
            SmokeParticle particle = new SmokeParticle((int) (Math.random() * 15) + 10, 0xFFFFFFFF);
            particle.setPos(getTransform().getPos());
            particle.setScale(1.5f);
            ((WorldClient) getWorld()).addParticle(particle);
            sprintParticle = SPRINT_PARTICLE_DELAY;
        }
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
    protected Vector3f tickTerrainCollisions(AABB aabb, Vector3f motion) {
        if (abilities.get(Abilities.Ability.NOCLIP)) {
            this.onGround = false;
            return new Vector3f(motion);
        }

        return super.tickTerrainCollisions(aabb, motion);
    }

    @Override
    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        if (!abilities.get(Abilities.Ability.NOCLIP))
            super.tickEntityCollisions(aabb, toMove);
    }

    @Override
    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        if (invulnerability > 0 || (getAbilities().get(Abilities.Ability.GOD_MODE) && type != DamageType.GOD))
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

        Vector3f diff = damageSource.getTransform().getPos().sub(transform.getPos(), new Vector3f());
        if (diff.lengthSquared() > 0f)
            diff.normalize();

        return Maths.dirToRot(diff).y - Maths.getYaw(getTransform().getRot());
    }

    public int getDamageSourceTicks() {
        return damageSourceTicks;
    }

    public void updateMovementFlags(boolean sneaking, boolean sprinting, boolean flying) {
        this.sneaking = sneaking;
        this.sprinting = (this.sprinting || sprinting) && !sneaking && forwards && !isRiding();
        this.flying = (flying && abilities.get(Abilities.Ability.CAN_FLY)) || abilities.get(Abilities.Ability.NOCLIP);

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

    @Override
    protected float getStepHeight() {
        return 1f; //0.3f;
    }

    @Override
    public float getPickRange() {
        return abilities.get(Abilities.Ability.CAN_FLY) ? super.getPickRange() : 3.5f;
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
    public void calculateBounds() {
        aabb.set(transform.getPos());
        float w = Math.max(DIMENSIONS.x, DIMENSIONS.z) * 0.5f;
        float y = model.getAABB().getHeight(); //Math.min(, DIMENSIONS.y);
        aabb.inflate(w, 0, w, w, y, w);
        aabb.scaleAnchorBottom(transform.getScale());
    }
}
