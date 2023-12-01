package mayo.world.entity.living;

import mayo.registry.EntityRegistry;
import mayo.registry.LivingModelRegistry;
import mayo.utils.Maths;
import mayo.world.DamageType;
import mayo.world.WorldClient;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

import java.util.UUID;

public class Player extends LivingEntity {

    private static final int MAX_HEALTH = 100;
    private static final int INVULNERABILITY_TIME = 10;
    private static final int INVENTORY_SIZE = 9;

    private int invulnerability = 0;
    private Entity damageSource;
    private int damageSourceTicks = 0;

    private boolean sprinting, sneaking, flying;

    public Player(UUID uuid) {
        this(uuid, LivingModelRegistry.STRAWBERRY);
    }

    public Player(UUID uuid, LivingModelRegistry model) {
        super(uuid, model == null ? LivingModelRegistry.random() : model, MAX_HEALTH, INVENTORY_SIZE);
    }

    @Override
    public void tick() {
        if (getWorld() instanceof WorldClient wc && wc.player == this && super.isRemoved())
            return;

        super.tick();

        if (invulnerability > 0)
            invulnerability--;

        if (damageSourceTicks > 0)
            damageSourceTicks--;
    }

    @Override
    protected void applyForces() {
        if (!flying) super.applyForces();
    }

    @Override
    protected void applyMovement() {
        if (flying) {
            this.motion.add(move);
        } else {
            super.applyMovement();
        }
    }

    @Override
    protected void motionFallout() {
        if (flying) {
            this.motion.mul(0.6f, 0.6f, 0.6f);
        } else {
            super.motionFallout();
        }
    }

    @Override
    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        if (type == DamageType.MELEE) {
            if (invulnerability > 0)
                return false;

            this.invulnerability = INVULNERABILITY_TIME;
        }

        boolean result = super.damage(source, type, amount, crit);

        if (result) {
            this.damageSource = source;
            this.damageSourceTicks = 30;
        }

        return result;
    }

    @Override
    protected void spawnDeathParticles() {
        if (!(getWorld() instanceof WorldClient wc) || wc.player != this || wc.isThirdPerson())
            super.spawnDeathParticles();
    }

    @Override
    protected void spawnHealthChangeParticle(int amount, boolean crit) {
        if (!(getWorld() instanceof WorldClient wc) || wc.player != this || wc.isThirdPerson())
            super.spawnHealthChangeParticle(amount, crit);
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
        this.sprinting = !sneaking && (this.sprinting || sprinting);
        this.flying = flying;

        if (this.isRiding() && sneaking)
            this.stopRiding();
    }

    @Override
    public void move(float left, float up, float forwards) {
        super.move(left, up, forwards);

        if (forwards <= 0f)
            sprinting = false;

        if (flying)
            move.y = Math.signum(up) * 0.15f;
    }

    @Override
    protected float getMoveSpeed() {
        float speed = super.getMoveSpeed();

        if (sneaking)
            speed *= 0.5f;
        if (sprinting)
            speed *= flying ? 2.3f : 1.3f;

        return speed;
    }

    private float clampPitch(float pitch) {
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

    @Override
    public boolean isRemoved() {
        return super.isRemoved() && !(getWorld() instanceof WorldClient wc && wc.player == this);
    }
}
