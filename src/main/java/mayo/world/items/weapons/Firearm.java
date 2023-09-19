package mayo.world.items.weapons;

import mayo.world.items.Item;

public class Firearm extends Item {

    public Firearm(String id, int maxRounds) {
        super(id, maxRounds);
        reload();
    }

    public boolean shoot() {
        if (count > 0) {
            count--;
            return true;
        }
        return false;
    }

    public void reload() {
        count = stackCount;
    }
}
