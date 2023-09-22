package mayo.world.entity;

import mayo.model.LivingEntityModels;
import mayo.world.World;

public class Player extends LivingEntity {

    private static final int MAX_HEALTH = 100;
    private static final int INVULNERABILITY_TIME = 20;

    private int invulnerability = 0;

    public Player(World world) {
        super(LivingEntityModels.PICKLE, world, MAX_HEALTH);
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
    public void damage(int amount) {
        if (invulnerability > 0)
            return;

        super.damage(amount);
        this.invulnerability = INVULNERABILITY_TIME;
    }
}
