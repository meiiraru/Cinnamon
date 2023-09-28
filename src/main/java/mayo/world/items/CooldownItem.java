package mayo.world.items;

public abstract class CooldownItem extends Item {

    private final int depleatCooldown, useCooldown;
    private int depleat, use;

    public CooldownItem(String id, int stackCount, int depleatCooldown, int useCooldown) {
        super(id, stackCount);
        this.depleatCooldown = depleatCooldown;
        this.useCooldown = useCooldown;
    }

    @Override
    public void tick() {
        super.tick();
        if (isOnCooldown()) {
            depleat--;
            if (!isOnCooldown())
                onCooldownEnd();
        } else if (!canUse()) {
            use--;
        }
    }

    public void onCooldownEnd() {
        use = 0;
    }

    public int getCooldown() {
        return depleat;
    }

    public int getCooldownTime() {
        return depleatCooldown;
    }

    public void setOnCooldown() {
        this.depleat = depleatCooldown;
    }

    public boolean isOnCooldown() {
        return this.depleat > 0;
    }

    public float getCooldownProgress() {
        return 1 - (float) getCooldown() / getCooldownTime();
    }

    public int getUseCooldown() {
        return this.useCooldown;
    }

    public boolean canUse() {
        return this.use <= 0;
    }

    public void setUseCooldown() {
        this.use = this.useCooldown;
    }
}
