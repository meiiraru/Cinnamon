package cinnamon.world.items;

import cinnamon.math.Maths;

/**
 * Inventory class that holds a list of {@link Item}<br>
 * The inventory has a hotbar and a backpack, the hotbar takes the first <i>n</i> slots of the inventory<br>
 * Items take priority in the hotbar, and if the hotbar is full, they will be added to the backpack
 */
public class Inventory {

    private int size;
    private int hotbarSize;
    private Item[] items;
    private int selected;

    /**
     * Creates an inventory containing only a hotbar
     * @param slots the size of the inventory
     */
    public Inventory(int slots) {
        this(slots, 0);
    }

    /**
     * Creates an inventory with a hotbar and a backpack
     * @param hotbarSize the size of the hotbar
     * @param backpackSize the size of the backpack
     */
    public Inventory(int hotbarSize, int backpackSize) {
        this.hotbarSize = hotbarSize;
        this.size = hotbarSize + backpackSize;
        this.items = new Item[size];
    }

    /**
     * Ticks all the items in the inventory, and removes any that have a count of 0
     */
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
     * Adds an {@link Item} to the inventory, merging it with existing stacks if possible<br>
     * If the item cannot be fully added, it will be partially added and the remaining count will be left in the item stack<br>
     * The item will be added to the selected hotbar slot first, then to the inventory, and finally to the first free slot<br>
     * If the item cannot be added at all, it will be left in the item stack
     * @param item the item to add
     * @return 0 if the item was not added<br>
     * 1 if it was partially added<br>
     * 2 if it was fully added
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

    /**
     * Sets an {@link Item} in the inventory at the specified slot<br>
     * This will swap any existing item in that slot
     * @param slot the slot to set the item in
     * @param item the item to set
     * @return the item that was previously in that slot, or null if there was no item
     */
    public Item setItem(int slot, Item item) {
        Item previous = items[slot];
        items[slot] = item;
        return previous;
    }

    /**
     * Gets an {@link Item} from the inventory at the specified slot
     * @param slot the slot to get the item from
     * @return the item in that slot, or {@code null} if there is no item
     */
    public Item getItem(int slot) {
        return slot >= 0 && slot < size ? items[slot] : null;
    }

    /**
     * Checks if the inventory has an empty space for more items
     * @return {@code true} if there is space, {@code false} otherwise
     */
    public boolean hasSpace() {
        return getFreeIndex() != -1;
    }

    /**
     * Gets the index of the first free slot in the inventory
     * @return the index of the first free slot, or -1 if there is no free slot
     */
    public int getFreeIndex() {
        for (int i = 0; i < size; i++) {
            if (items[i] == null)
                return i;
        }

        return -1;
    }

    /**
     * Gets all the {@link Item} in the inventory
     * @return an array of all the items in the inventory
     */
    public Item[] getItems() {
        return items;
    }

    /**
     * Gets the currently selected {@link Item} in the hotbar
     * @return the selected item, or {@code null} if there is no item in the selected slot
     */
    public Item getSelectedItem() {
        return selected >= 0 && selected < size ? items[selected] : null;
    }

    /**
     * Sets the selected index in the hotbar, wrapping around through the hotbar size
     * @param index the index to set the selected slot to
     */
    public void setSelectedIndex(int index) {
        this.selected = Maths.modulo(index, hotbarSize);
    }

    /**
     * Gets the currently selected index in the hotbar
     * @return the selected index
     */
    public int getSelectedIndex() {
        return selected;
    }

    /**
     * Gets the size of the inventory
     * @return the size of the inventory
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size of the inventory, removing any items when shrinking the inventory<br>
     * The removed items will be returned in an array, and the inventory will be updated to the new size<br>
     * The hotbar size will be clamped to the new inventory size
     * @param size the new size of the inventory
     * @return an array of the items that were removed from the inventory when shrinking it
     */
    public Item[] setSize(int size) {
        //store the items that will be removed from shrinking the inventory
        Item[] removedItems = new Item[Math.max(0, this.size - size)];
        if (size < this.size)
            System.arraycopy(items, size, removedItems, 0, removedItems.length);

        //create a new array with the size and items of the new inventory
        Item[] newItems = new Item[size];
        System.arraycopy(items, 0, newItems, 0, Math.min(this.size, size));

        //update the inventory
        this.size = size;
        this.hotbarSize = Math.min(hotbarSize, size);
        this.items = newItems;

        //return the removed items
        return removedItems;
    }

    /**
     * Gets the size of the hotbar
     * @return the size of the hotbar
     */
    public int getHotbarSize() {
        return hotbarSize;
    }

    /**
     * Sets the size of the hotbar, limiting it to the size of the inventory
     * @param hotbarSize the new size of the hotbar
     */
    public void setHotbarSize(int hotbarSize) {
        this.hotbarSize = Math.min(hotbarSize, size);
    }
}
