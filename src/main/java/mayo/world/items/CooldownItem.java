package mayo.world.items;

public abstract class CooldownItem extends Item {

    private final int cooldownTime;
    private int cooldown;

    public CooldownItem(String id, int stackCount, int cooldownTime) {
        super(id, stackCount);
        this.cooldownTime = cooldownTime;
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

    public void onCooldownEnd() {}

    public int getCooldown() {
        return cooldown;
    }

    public int getCooldownTime() {
        return cooldownTime;
    }

    protected void resetCooldown() {
        this.cooldown = cooldownTime;
    }

    public boolean isOnCooldown() {
        return this.cooldown > 0;
    }

    public float getCooldownProgress() {
        return 1 - (float) getCooldown() / getCooldownTime();
    }
}
