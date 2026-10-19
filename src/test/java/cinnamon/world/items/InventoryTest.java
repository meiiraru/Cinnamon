package cinnamon.world.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Inventory Tests")
class InventoryTest {

    private Inventory inventory;

    //pure MVP item, ignoring category and skipping model rendering
    static class DummyItem extends Item {
        public DummyItem(int count) {
            super("Dummy Item", count, 99, null);
        }

        @Override
        public Item copy() {
            return new DummyItem(getCount());
        }

        @Override
        public ItemCategory getCategory() {
            return ItemCategory.UNKNOWN;
        }
    }

    @BeforeEach
    void setUp() {
        //create a new inventory with 5 slots for each test
        inventory = new Inventory(5);
    }

    @Nested
    @DisplayName("Initialization & Basic Slots")
    class InitializationTests {

        @Test
        @DisplayName("Initialization state")
        void testInitialState() {
            //check inventory initialization state
            assertEquals(5, inventory.getSize());
            assertTrue(inventory.hasSpace());
            assertEquals(0, inventory.getFreeIndex());

            //check inventory items state
            for (int i = 0; i < inventory.getSize(); i++)
                assertNull(inventory.getItem(i));
        }

        @Test
        @DisplayName("Item insertion and retrieval")
        void testSetAndGetItem() {
            //create a dummy item at slot 2
            DummyItem item = new DummyItem(10);
            inventory.setItem(2, item);

            //check if the item is correctly set at the correct position
            assertSame(item, inventory.getItem(2));
            assertNull(inventory.getItem(0));
        }

        @Test
        @DisplayName("Item removal from full inventory")
        void testSpaceAndFreeIndex() {
            //fill inventory
            for (int i = 0; i < 5; i++)
                inventory.setItem(i, new DummyItem(99));

            //check that inventory is full
            assertFalse(inventory.hasSpace());
            assertEquals(-1, inventory.getFreeIndex());

            //clear a slot and check again
            inventory.setItem(2, null);
            assertTrue(inventory.hasSpace());
            assertEquals(2, inventory.getFreeIndex());
        }
    }

    @Nested
    @DisplayName("Selection Handling")
    class SelectionTests {

        @Test
        @DisplayName("Select Item")
        void testSelection() {
            //set an item in slot 1 and select it
            DummyItem item = new DummyItem(5);
            inventory.setItem(1, item);
            inventory.setSelectedIndex(1);

            //check that the selected index and item are correct
            assertEquals(1, inventory.getSelectedIndex());
            assertSame(item, inventory.getSelectedItem());
        }

        @Test
        @DisplayName("Selected Index Wrapping")
        void testIndexWrapping() {
            //check for out of bounds selection and wrapping behavior
            //5 % 5 = 0
            inventory.setSelectedIndex(5);
            assertEquals(0, inventory.getSelectedIndex());

            //7 % 5 = 2
            inventory.setSelectedIndex(7);
            assertEquals(2, inventory.getSelectedIndex());

            //-1 % 5 = 4 (true modulo)
            inventory.setSelectedIndex(-1);
            assertEquals(4, inventory.getSelectedIndex());

            //-6 % 5 = 4 (true modulo)
            inventory.setSelectedIndex(-6);
            assertEquals(4, inventory.getSelectedIndex());
        }
    }

    @Nested
    @DisplayName("Merged Item Insertion")
    class PutItemTests {

        @Test
        @DisplayName("Skip Empty Item")
        void testPutZeroCountItem() {
            //add an empty item to the inventory
            DummyItem emptyItem = new DummyItem(0);
            int result = inventory.putItem(emptyItem);

            //check that the result is 0 (not added) and that the inventory remains unchanged
            assertEquals(0, result);
            assertTrue(inventory.hasSpace());
            assertNull(inventory.getItem(0));
        }

        @Test
        @DisplayName("Insert into Empty Slot")
        void testPutItemInEmptySlot() {
            //insert the item into the inventory
            DummyItem item = new DummyItem(50);
            int result = inventory.putItem(item);

            //check that the result is 2 (fully added)
            //and the item is correctly placed in the first free slot (slot 0)
            assertEquals(2, result);
            assertSame(item, inventory.getItem(0));
            assertEquals(50, item.getCount());
        }

        @Test
        @DisplayName("Merge into Existing Item")
        void testPutItemMergeFully() {
            //first item taking 50 out of 99
            inventory.setItem(0, new DummyItem(50));

            //insert an incoming item that can fully merge into the existing item
            DummyItem incoming = new DummyItem(40);
            int result = inventory.putItem(incoming);

            //check that the result is 2 (fully added)
            //the existing item count is updated
            //and the incoming item is depleted and not added to any other slot
            assertEquals(2, result);
            assertEquals(90, inventory.getItem(0).getCount());
            assertEquals(0, incoming.getCount());
            assertNull(inventory.getItem(1));
        }

        @Test
        @DisplayName("Item Merging Selected Priority")
        void testMergeIntoSelectedItemFirst() {
            //set up two items in the inventory, and select the slot id 1
            inventory.setItem(0, new DummyItem(50));
            inventory.setItem(1, new DummyItem(50));
            inventory.setSelectedIndex(1);

            //add an incoming item that can merge into both existing items
            DummyItem incoming = new DummyItem(20);
            int result = inventory.putItem(incoming);

            //except the item to be fully added (2)
            //and expect that the selected item (slot 1) received the merge
            assertEquals(2, result);
            assertEquals(70, inventory.getItem(1).getCount());
            assertEquals(50, inventory.getItem(0).getCount());
            assertEquals(0, incoming.getCount());
        }

        @Test
        @DisplayName("Item Merge & Split")
        void testMergeOverflowIntoFreeSlot() {
            //new item with 90 out of 99 count
            inventory.setItem(0, new DummyItem(90));

            //9 can merge, 11 will overflow into a new slot
            DummyItem incoming = new DummyItem(20);
            int result = inventory.putItem(incoming);

            //expect to be fully added (2), with 9 merged into slot 0 (99) and 11 placed into slot 1
            assertEquals(2, result);
            assertEquals(99, inventory.getItem(0).getCount());
            assertSame(incoming, inventory.getItem(1));
            assertEquals(11, inventory.getItem(1).getCount());
        }

        @Test
        @DisplayName("Partially Merged Item with Inventory No Space")
        void testPutItemPartiallyAddedNoSpace() {
            //fill all 5 slots
            //slot 0 has 90, slots 1-4 have 99
            inventory.setItem(0, new DummyItem(90));
            for (int i = 1; i < 5; i++)
                inventory.setItem(i, new DummyItem(99));

            //add an incoming item with 20 count
            DummyItem incoming = new DummyItem(20);
            int result = inventory.putItem(incoming);

            //expect to be partially added (1), with 9 merged into slot 0 (99) and 11 remaining in the incoming item
            assertEquals(1, result);
            assertEquals(99, inventory.getItem(0).getCount());
            assertEquals(11, incoming.getCount());
        }

        @Test
        @DisplayName("Merge Item with Full Inventory")
        void testPutItemCompletelyFull() {
            //fill all inventory slots with max
            for (int i = 0; i < 5; i++)
                inventory.setItem(i, new DummyItem(99));

            //try to add a new item
            DummyItem incoming = new DummyItem(10);
            int result = inventory.putItem(incoming);

            //expect that the item cannot be added (0), and the incoming item remains untouched
            assertEquals(0, result);
            assertEquals(10, incoming.getCount());
        }
    }

    @Nested
    @DisplayName("Tick Handling")
    class TickTests {

        @Test
        @DisplayName("Tick remove empty items")
        void testTickRemovesEmptyItems() {
            //create two items, one active and one depleted
            DummyItem active = new DummyItem(10);
            DummyItem depleted = new DummyItem(0);

            //set them in the inventory
            inventory.setItem(0, active);
            inventory.setItem(1, depleted);

            //call tick to process the inventory
            inventory.tick();

            //expect that the active item remains and the depleted item is cleared
            assertNotNull(inventory.getItem(0));
            assertEquals(10, inventory.getItem(0).getCount());
            assertNull(inventory.getItem(1), "Slot 1 should be cleared when count <= 0");
        }
    }

    @Nested
    @DisplayName("Inventory Resizing")
    class ResizeTests {

        @Test
        @DisplayName("Expanding Inventory")
        void testExpandSize() {
            //set items in the initial inventory
            inventory.setItem(0, new DummyItem(10));
            inventory.setItem(4, new DummyItem(20));

            //expand the inventory size from 5 to 10
            inventory.setSize(10);

            //check that the size is updated and existing items are untouched
            assertEquals(10, inventory.getSize());
            assertNotNull(inventory.getItem(0));
            assertNotNull(inventory.getItem(4));
            assertNull(inventory.getItem(5));
        }

        @Test
        @DisplayName("Shrinking Inventory")
        void testShrinkSize() {
            //set items in the initial inventory
            inventory.setItem(0, new DummyItem(10));
            inventory.setItem(1, new DummyItem(20));
            inventory.setItem(4, new DummyItem(30));

            //shrink the inventory size from 5 to 2
            inventory.setSize(2);

            //check that the size is updated and items beyond the new size are cleared
            assertEquals(2, inventory.getSize());
            assertNotNull(inventory.getItem(0));
            assertNotNull(inventory.getItem(1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> inventory.getItem(4));
        }
    }
}