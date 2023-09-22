package mayo.world.items;

import mayo.world.entity.Entity;

public abstract class Item {

    private final String id;
    protected final int stackCount;
    protected int count = 1;

    public Item(String id, int stackCount) {
        this.id = id;
        this.stackCount = stackCount;
    }

    public void tick() {}

    public void attack(Entity source) {}

    public void use(Entity source) {}

    public String getId() {
        return id;
    }

    public int getStackCount() {
        return stackCount;
    }

    public int getCount() {
        return count;
    }

    public boolean hasAttack() {
        return false;
    }
}
