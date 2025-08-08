package cinnamon.world.items;

public class Inventory {

    private final int size;
    private final Item[] items;
    private int selected;

    public Inventory(int slots) {
        this.size = slots;
        this.items = new Item[slots];
    }

    public void tick() {
        for (Item item : items) {
            if (item != null)
                item.tick();
        }
    }

    public int putItem(Item item) {
        int i = getFreeIndex();
        if (i >= 0)
            setItem(i, item);
        return i;
    }

    public void setItem(int slot, Item item) {
        items[slot] = item;
    }

    public Item getItem(int slot) {
        return items[slot];
    }

    public boolean hasSpace() {
        for (Item item : items) {
            if (item == null)
                return true;
        }
        return false;
    }

    public int getFreeIndex() {
        for (int i = 0; i < size; i++) {
            if (items[i] == null)
                return i;
        }

        return -1;
    }

    public Item[] getItems() {
        return items;
    }

    public Item getSelectedItem() {
        return items[selected];
    }

    public void setSelectedIndex(int index) {
        this.selected = ((index % size) + size) % size;
    }

    public int getSelectedIndex() {
        return selected;
    }

    public int getSize() {
        return size;
    }
}
