package mayo.world.entity.living;

import mayo.model.ModelRegistry;
import mayo.utils.Meth;
import mayo.world.DamageType;
import mayo.world.World;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Player extends LivingEntity {

    private static final int MAX_HEALTH = 100;
    private static final int INVULNERABILITY_TIME = 10;
    private static final int INVENTORY_SIZE = 9;

    private int invulnerability = 0;
    private Entity damageSource;
    private int damageSourceTicks = 0;

    private boolean sprinting, sneaking;

    public Player(World world, ModelRegistry.Living model) {
        super(model == null ? ModelRegistry.Living.random() : model, world, MAX_HEALTH, INVENTORY_SIZE);
    }

    @Override
    public void tick() {
        super.tick();

        if (invulnerability > 0)
            invulnerability--;

        if (damageSourceTicks > 0)
            damageSourceTicks--;
    }

    @Override
    public boolean shouldRenderText() {
        return false;
    }

    @Override
    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        if (type == DamageType.MELEE) {
            if (invulnerability > 0)
                return false;

            this.invulnerability = INVULNERABILITY_TIME;
        }

        this.damageSource = source;
        this.damageSourceTicks = 30;
        return super.damage(source, type, amount, crit);
    }

    @Override
    protected void spawnDeathParticles() {
        if (world.isThirdPerson())
            super.spawnDeathParticles();
    }

    @Override
    protected void spawnHealthChangeParticle(int amount, boolean crit) {
        if (world.isThirdPerson())
            super.spawnHealthChangeParticle(amount, crit);
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    public Float getDamageAngle() {
        if (damageSource == null)
            return null;

        Vector3f diff = damageSource.getPos().sub(pos, new Vector3f()).normalize();
        return Meth.dirToRot(diff).y - rot.y;
    }

    public int getDamageSourceTicks() {
        return damageSourceTicks;
    }

    public void updateMovementFlags(boolean sneaking, boolean sprinting) {
        this.sneaking = sneaking;
        this.sprinting = sprinting;
    }

    @Override
    protected float getMoveSpeed() {
        return super.getMoveSpeed() * (sneaking ? 0.5f : sprinting ? 2f : 1f);
    }
}
