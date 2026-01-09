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
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            if (item == null)
                continue;

            item.tick();
            if (item.getCount() <= 0) {
                item.unselect();
                items[i] = null;
            }
        }
    }

    public int putItem(Item item) {
        int state = 0;
        int freeIndex = -1;
        int remaining = item.getCount();

        for (int i = 0; i < size; i++) {
            Item invItem = items[i];

            //no item
            if (invItem == null) {
                //save free index
                if (freeIndex == -1) freeIndex = i;
                continue;
            }

            //no stack compatible
            if (!invItem.stacksWith(item))
                continue;

            //found stack compatible, try to add to it
            int space = invItem.getStackSize() - invItem.getCount();
            if (space > 0) {
                int toAdd = Math.min(space, remaining);
                invItem.setCount(invItem.getCount() + toAdd);
                remaining -= toAdd;

                //all added - else continue loop and try to find the next stack
                if (remaining == 0)
                    return 2;
            }
        }

        //partially added the item stack
        if (remaining < item.getCount())
            state = 1;

        //update item stack count
        item.setCount(remaining);

        //could not add to any existing stacks, add to free index
        if (remaining > 0 && freeIndex != -1) {
            items[freeIndex] = item;
            return 2;
        }

        //item was not entirely added
        return state;
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
