package mayo.world.items.weapons;

import mayo.world.items.CooldownItem;

public class Firearm extends CooldownItem {

    public Firearm(String id, int maxRounds, int reloadTime) {
        super(id, maxRounds, reloadTime);
        reload();
    }

    @Override
    public void attack() {
        super.attack();

        if (isOnCooldown())
            return;

        if (!shoot())
            this.resetCooldown();
    }

    @Override
    public boolean hasAttack() {
        return true;
    }

    @Override
    public void onCooldownEnd() {
        super.onCooldownEnd();
        reload();
    }

    private boolean shoot() {
        if (count > 0)
            count--;

        return count > 0;
    }

    private void reload() {
        count = stackCount;
    }
}
