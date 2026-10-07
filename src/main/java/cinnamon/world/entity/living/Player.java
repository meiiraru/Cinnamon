package cinnamon.world.entity.living;

import cinnamon.math.Maths;
import cinnamon.math.collision.Collider;
import cinnamon.math.collision.shape.AABB;
import cinnamon.registry.EntityRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.settings.Settings;
import cinnamon.utils.Resource;
import cinnamon.world.Abilities;
import cinnamon.world.entity.DamageType;
import cinnamon.world.entity.Entity;
import cinnamon.world.particle.SmokeParticle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class Player extends LivingEntity {

    public static final int MAX_HEALTH = 100;
    public static final int INVULNERABILITY_TIME = 10;
    public static final int INVENTORY_SIZE = 9;
    public static final int SPRINT_PARTICLE_DELAY = 3;
    public static final Vector3f DIMENSIONS = new Vector3f(0.6f, 1.8f, 0.6f);

    private final Abilities abilities = new Abilities();

    protected int invulnerability = 0;
    protected int damageSourceTicks = 0;
    protected Entity damageSource;

    private int sprintParticle = 0;

    protected boolean sprinting, sneaking, flying;
    protected boolean checkSneak;
    protected boolean pressingSneak, pressingForwards;

    public Player(String name, UUID uuid) {
        this(name, uuid, LivingModelRegistry.STRAWBERRY);
    }

    public Player(String name, UUID uuid, LivingModelRegistry model) {
        this(name, uuid, model.resource, model.eyeHeight);
    }

    public Player(String name, UUID uuid, Resource model, float eyeHeight) {
        super(uuid, model, eyeHeight, MAX_HEALTH, INVENTORY_SIZE);
        this.setName(name);
        this.getController().bindDoubleClick(
                "fly_toggle", Settings.jump.get(),
                click -> {
                    if (click)
                        updateMovementFlags(this.sneaking, this.sprinting, !this.flying);
                }
        ).bindState(
                "sneak", Settings.sneak.get(),
                sneaking -> {
                    pressingSneak = sneaking;
                    updateMovementFlags(sneaking, this.sprinting, this.flying);
                }
        ).bindState(
                "sprint", Settings.sprint.get(),
                sprinting -> {
                    pressingForwards = Settings.forward.get().isPressed() && !Settings.backward.get().isPressed();
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

        if (isFlying() && (isOnGround() || isRiding()))
            flying = false;

        if (this.isSprinting() && isOnGround() && --sprintParticle <= 0) {
            SmokeParticle particle = new SmokeParticle((int) (Math.random() * 15) + 10, 0xFFFFFFFF);
            particle.setPos(getTransform().getPos());
            particle.setScale(1.5f);
            ((WorldClient) getWorld()).addParticle(particle);
            sprintParticle = SPRINT_PARTICLE_DELAY;
        }

        this.scaleTo(1f, isSneaking() ? 0.75f : 1f, 1f);
    }

    @Override
    protected void applyForces() {
        if (!isFlying()) super.applyForces();
    }

    @Override
    protected void applyImpulse() {
        if (isFlying()) {
            this.motion.add(impulse);
            this.impulse.set(0);
        } else {
            super.applyImpulse();
        }
    }

    @Override
    protected void motionFallout() {
        if (isFlying()) {
            this.motion.mul(0.6f);
        } else {
            super.motionFallout();
        }
    }

    @Override
    protected Vector3f tickTerrainCollisions(AABB aabb, Vector3f motion) {
        if (getAbilities().get(Abilities.Ability.NOCLIP)) {
            this.onGround = false;
            return new Vector3f(motion);
        }

        return super.tickTerrainCollisions(aabb, motion);
    }

    @Override
    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        if (!getAbilities().get(Abilities.Ability.NOCLIP))
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
        this.sprinting = (this.isSprinting() || sprinting) && !sneaking && pressingForwards && !isRiding();
        this.flying = (flying && getAbilities().get(Abilities.Ability.CAN_FLY)) || getAbilities().get(Abilities.Ability.NOCLIP);

        this.checkSneak |= sneaking;

        if (this.isRiding() && sneaking)
            this.stopRiding();
    }

    @Override
    public void impulse(float left, float up, float forwards) {
        super.impulse(left, up, forwards);

        if (isFlying())
            impulse.y = Math.signum(up) * 0.15f;
    }

    @Override
    protected float getMoveSpeed() {
        float speed = super.getMoveSpeed();

        if (isSneaking())
            speed *= getSneakingMultiplier();
        if (isSprinting())
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
        return getAbilities().get(Abilities.Ability.CAN_FLY) ? super.getPickRange() : 3.5f;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.PLAYER;
    }

    public boolean isSneaking() {
        return sneaking || (checkSneak && cannotUnsneak());
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
        AABB bb = getAABB().set(getTransform().getPos());
        float w = Math.max(DIMENSIONS.x, DIMENSIONS.z) * 0.5f;
        float y = model.getAABB().getHeight(); //Math.min(, DIMENSIONS.y);
        bb.inflate(w, 0, w, w, y, w);
        bb.scaleAnchorBottom(getTransform().getScale());
    }

    protected boolean cannotUnsneak() {
        //were riding, sneaking is not allowed
        if (isRiding()) {
            checkSneak = false;
            return false;
        }

        //prepare bounds for terrain check
        Vector3f pos = getTransform().getPos();
        float w = Math.max(DIMENSIONS.x, DIMENSIONS.z) * 0.5f;
        float h = model.getAABB().getHeight();
        AABB bb = new AABB(pos.x - w, pos.y, pos.z - w, pos.x + w, pos.y + h, pos.z + w);

        //check if there is terrain in the bounds
        boolean hasTerrain = false;
        for (Terrain terrain : getWorld().getTerrains(bb)) {
            for (Collider<?> collider : terrain.getPreciseCollider()) {
                if (collider.intersects(bb)) {
                    hasTerrain = true;
                    break;
                }
            }
        }

        checkSneak = hasTerrain;
        return hasTerrain;
    }
}
