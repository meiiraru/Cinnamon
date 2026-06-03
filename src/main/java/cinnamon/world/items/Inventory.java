package cinnamon.world.items;

public class Inventory {

    private int size;
    private Item[] items;
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

    /**
     * @return 0 if the item was not added<br>1 if it was partially added<br>2 if it was fully added
     */
    public int putItem(Item item) {
        int state = 0;
        int freeIndex = -1;
        int initialCount = item.getCount();

        //invalid item
        if (initialCount == 0)
            return 0;

        //try to merge with selected item first
        Item selectedItem = getSelectedItem();
        if (selectedItem != null) {
            selectedItem.mergeItem(item);
            if (item.getCount() == 0)
                return 2;
        }

        //try to add to the inventory
        for (int i = 0; i < size; i++) {
            Item invItem = items[i];

            //no item
            if (invItem == null) {
                //save free index
                if (freeIndex == -1) freeIndex = i;
                continue;
            }

            //merge with the item
            invItem.mergeItem(item);
            if (item.getCount() == 0)
                return 2;
        }

        //could not add to any existing stacks, and there was a free index
        if (freeIndex != -1) {
            items[freeIndex] = item;
            return 2;
        }

        //partially added the item stack
        if (item.getCount() < initialCount)
            state = 1;

        //item was partially added (1) or not added at all (0)
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
        return selected >= 0 && selected < size ? items[selected] : null;
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

    public void setSize(int size) {
        Item[] newItems = new Item[size];
        System.arraycopy(items, 0, newItems, 0, Math.min(this.size, size));
        this.size = size;
        this.items = newItems;
    }
}
