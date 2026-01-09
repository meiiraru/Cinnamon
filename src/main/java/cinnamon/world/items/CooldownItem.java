package cinnamon.world.items;

import cinnamon.utils.Resource;

public abstract class CooldownItem extends Item {

    protected final int cooldownTime;
    protected int cooldown;

    public CooldownItem(String id, int count, int stackCount, Resource model, int cooldown) {
        super(id, count, stackCount, model);
        this.cooldownTime = cooldown;
    }

    @Override
    public void tick() {
        super.tick();
        if (isOnCooldown()) {
            cooldown--;
            if (!isOnCooldown())
                onCooldownEnd();
        }
    }

    @Override
    public boolean isFiring() {
        return !isOnCooldown() && super.isFiring();
    }

    @Override
    public boolean isUsing() {
        return !isOnCooldown() && super.isUsing();
    }

    public void onCooldownEnd() {}

    public int getCooldown() {
        return cooldown;
    }

    public int getCooldownTime() {
        return cooldownTime;
    }

    public void setOnCooldown() {
        this.cooldown = cooldownTime;
    }

    protected void breakCooldown() {
        if (isOnCooldown())
            this.cooldown = 0;
    }

    public boolean isOnCooldown() {
        return this.cooldown > 0;
    }

    public float getCooldownProgress() {
        return 1 - (float) getCooldown() / getCooldownTime();
    }
}
