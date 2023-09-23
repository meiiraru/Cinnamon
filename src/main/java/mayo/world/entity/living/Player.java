package mayo.world.entity.living;

import mayo.model.LivingEntityModels;
import mayo.world.World;

public class Player extends LivingEntity {

    private static final int MAX_HEALTH = 100;
    private static final int INVULNERABILITY_TIME = 10;

    private int invulnerability = 0;

    public Player(World world) {
        super(LivingEntityModels.STRAWBERRY, world, MAX_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();

        if (invulnerability > 0)
            invulnerability--;
    }

    @Override
    public boolean shouldRenderText() {
        return false;
    }

    @Override
    public void damage(int amount, boolean crit) {
        if (invulnerability > 0)
            return;

        super.damage(amount, crit);
        this.invulnerability = INVULNERABILITY_TIME;
    }

    @Override
    protected void spawnDamageParticle(int diff, boolean crit) {
        if (world.isThirdPerson())
            super.spawnDamageParticle(diff, crit);
    }

    @Override
    public boolean isRemoved() {
        return false;
    }
}
