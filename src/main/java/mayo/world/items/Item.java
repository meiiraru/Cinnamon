package mayo.world.items;

public abstract class Item {

    private final String id;
    protected final int stackCount;
    protected int count = 1;

    public Item(String id, int stackCount) {
        this.id = id;
        this.stackCount = stackCount;
    }

    public String getId() {
        return id;
    }

    public int getStackCount() {
        return stackCount;
    }

    public int getCount() {
        return count;
    }
}
