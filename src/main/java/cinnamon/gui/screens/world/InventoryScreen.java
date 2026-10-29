package cinnamon.gui.screens.world;

import cinnamon.gui.GUISkin;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.Label;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.CoreShadersRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.settings.Settings;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import org.joml.Math;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class InventoryScreen extends Screen {

    public static final Resource
            BACKGROUND = new Resource("textures/gui/screens/inventory/background.png"),
            SLOT       = new Resource("textures/gui/screens/inventory/slot.png");

    protected LivingEntity entity;
    protected int hoveredSlot = -1;
    protected Item cursorItem;

    protected long lastClickTime = 0L;
    protected int lastClickButton = -1;
    protected int lastClickSlot = -1;

    @Override
    public void init() {
        super.init();
        //title
        Label label = new Label(width / 2, 32, Text.translated("gui.inventory").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER);
        addWidget(label);
    }

    @Override
    public void close() {
        if (cursorItem != null && entity != null) {
            if (entity.getInventory().putItem(cursorItem) < 2)
                entity.dropItem(cursorItem);
        }
        super.close();
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        updateInventory();
    }

    private void updateInventory() {
        if (!(client.world.cameraEntity instanceof LivingEntity le))
            this.close();
        else {
            entity = le;
        }
    }

    @Override
    protected void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderChildren(matrices, mouseX, mouseY, delta);

        if (entity == null)
            return;

        //get inventory size
        Inventory inventory = entity.getInventory();
        int invSize = inventory.getSize();
        int hotbarSize = inventory.getHotbarSize();
        int backpackSize = invSize - hotbarSize;

        //make a rectangular grid of the backpack size
        int columns = (int) Math.ceil(Math.sqrt(backpackSize));
        int rows = (int) Math.ceil((double) backpackSize / columns);

        //render backpack background
        int w = 16 * columns + 8;
        int h = 16 * rows + 8;
        if (backpackSize > 0) {
            UIHelper.nineQuad(
                    VertexConsumer.MAIN, matrices, BACKGROUND,
                    (width - w) / 2f, (height - h - 4f - 16f) / 2f,
                    w, h,
                    0f, 0f,
                    16, 16,
                    16, 16
            );
        }

        //render hotbar background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, BACKGROUND,
                (width - 16 * hotbarSize - 8) / 2f, (height + h + 4f - 16f) / 2f,
                16 * hotbarSize + 8, 16 + 8,
                0f, 0f,
                16, 16,
                16, 16
        );

        //render backpack slots
        for (int i = 0; i < columns * rows; i++) {
            int x = i % columns;
            int y = i / columns;
            VertexConsumer.MAIN.consume(GeometryHelper.quad(
                    matrices,
                    (width - w) / 2f + 4f + x * 16, (height - h - 4f - 16f) / 2f + 4f + y * 16,
                    16, 16,
                    i >= backpackSize ? 32f : hoveredSlot - hotbarSize == i ? 16f : 0f, 0f,
                    16, 16,
                    48, 16
            ), SLOT);
        }

        //draw hotbar slots
        for (int i = 0; i < hotbarSize; i++) {
            VertexConsumer.MAIN.consume(GeometryHelper.quad(
                    matrices,
                    (width - 16 * hotbarSize - 8) / 2f + 4f + i * 16, (height + h + 4f - 16f) / 2f + 4f,
                    16, 16,
                    hoveredSlot == i ? 16f : 0f, 0f,
                    16, 16,
                    48, 16
            ), SLOT);
        }

        //setup model shader
        CoreShadersRegistry.MODEL.getShader().use().setup(client.camera);

        //render all items
        for (int i = 0; i < invSize; i++) {
            Item item = inventory.getItem(i);
            if (item == null)
                continue;

            float slotX, slotY;

            //find the slot position for the item
            //hotbar
            if (i < hotbarSize) {
                slotX = (width - 16 * hotbarSize - 8) / 2f + 4f + i * 16;
                slotY = (height + h + 4f - 16f) / 2f + 4f;
            }
            //backpack
            else {
                int bpIndex = i - hotbarSize;
                int x = bpIndex % columns;
                int y = bpIndex / columns;
                slotX = (width - w) / 2f + 4f + x * 16;
                slotY = (height - h - 4f - 16f) / 2f + 4f + y * 16;
            }

            renderItem(matrices, item, slotX, slotY, delta);
        }

        if (cursorItem != null) {
            matrices.pushMatrix();
            matrices.translate(0, 0, UIHelper.getDepthOffset() + 5f);
            renderItem(matrices, cursorItem, mouseX - 8, mouseY - 8, delta);
            matrices.popMatrix();
        }

        //render hovered tooltip
        Item hoveredItem = inventory.getItem(hoveredSlot);
        if (hoveredItem != null) {
            matrices.pushMatrix();
            matrices.translate(0, 0, UIHelper.getDepthOffset() * 2f + 10f);
            UIHelper.renderTooltip(matrices, mouseX, mouseY - 24, Text.translated(hoveredItem.getId()));
            matrices.popMatrix();
        }
    }

    protected void renderItem(MatrixStack matrices, Item item, float x, float y, float delta) {
        matrices.pushMatrix();
        matrices.translate(x + 8, y + 8, 5f);
        matrices.rotateY(-90);
        matrices.rotateX(35);
        matrices.scale(-8);

        item.render(ItemRenderContext.HUD, matrices, delta);

        matrices.popMatrix();

        if (item.getStackSize() > 1) {
            matrices.pushMatrix();
            matrices.translate(x, y, UIHelper.getDepthOffset() + 10f);
            Text.of(item.getCount()).withStyle(Style.EMPTY.outlined(true))
                    .render(VertexConsumer.MAIN, matrices, 16, 16 + 2, Alignment.BOTTOM_RIGHT);
            matrices.popMatrix();
        }
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        renderSolidBackground(GUISkin.getCurrentSkin().getInt("world_screen_bg_color"));
    }

    @Override
    public boolean mouseMove(int x, int y) {
        boolean sup = super.mouseMove(x, y);
        if (sup) return true;

        hoveredSlot = -1;

        if (entity == null)
            return false;

        //get inventory size
        Inventory inventory = entity.getInventory();
        int invSize = inventory.getSize();
        int hotbarSize = inventory.getHotbarSize();
        int backpackSize = invSize - hotbarSize;

        int columns = (int) Math.ceil(Math.sqrt(backpackSize));
        int rows = (int) Math.ceil((double) backpackSize / columns);

        int w = 16 * columns + 8;
        int h = 16 * rows + 8;

        //get the starting positions of the backpack and hotbar
        float backpackStartX = (width - w) / 2f + 4f;
        float backpackStartY = (height - h - 4f - 16f) / 2f + 4f;

        float hotbarStartX = (width - 16 * hotbarSize - 8) / 2f + 4f;
        float hotbarStartY = (height + h + 4f - 16f) / 2f + 4f;

        //check backpack
        if (x >= backpackStartX && x < backpackStartX + 16 * columns && y >= backpackStartY && y < backpackStartY + 16 * rows) {
            int gridX = (int) ((x - backpackStartX) / 16);
            int gridY = (int) ((y - backpackStartY) / 16);
            int index = gridY * columns + gridX;

            //check if the index is within the backpack size
            if (index < backpackSize)
                hoveredSlot = hotbarSize + index;
            else
                hoveredSlot = -2;
        }
        //check hotbar
        else if (x >= hotbarStartX && x < hotbarStartX + 16 * hotbarSize && y >= hotbarStartY && y < hotbarStartY + 16) {
            hoveredSlot = (int) ((x - hotbarStartX) / 16);
        }

        return true;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (hoveredSlot == -2)
            return super.mousePress(button, action, mods);

        if (action == GLFW_RELEASE) {
            //double click detection
            long time = client.ticks;
            boolean doubleClick = time - lastClickTime < Settings.doubleClickTime.get() && lastClickButton == button && lastClickSlot == hoveredSlot;
            lastClickTime = time;
            lastClickButton = button;
            lastClickSlot = hoveredSlot;

            //pick all
            if (doubleClick && button == GLFW_MOUSE_BUTTON_1 && hoveredSlot >= 0 && cursorItem != null) {
                Inventory inventory = entity.getInventory();

                //collect all valid slots containing matching items
                List<Integer> matchingSlots = new ArrayList<>();
                for (int i = 0; i < inventory.getSize(); i++) {
                    Item invItem = inventory.getItem(i);
                    if (invItem != null && invItem.stacksWith(cursorItem))
                        matchingSlots.add(i);
                }

                //sort by item count ascending so we get the smallest stacks first
                matchingSlots.sort(Comparator.comparingInt(slot -> inventory.getItem(slot).getCount()));

                //gather items from the matching slots into the cursor item until it reaches its stack size
                for (int slot : matchingSlots) {
                    if (cursorItem.getCount() >= cursorItem.getStackSize())
                        break;

                    //take the stack from the inventory slot
                    Item invItem = inventory.getItem(slot);
                    int space = cursorItem.getStackSize() - cursorItem.getCount();
                    int take = Math.min(space, invItem.getCount());

                    cursorItem.setCount(cursorItem.getCount() + take);
                    invItem.setCount(invItem.getCount() - take);

                    //remove from the inventory if taken all
                    if (invItem.getCount() <= 0)
                        inventory.setItem(slot, null);
                }

                return true;
            }

            //out of inventory bounds, drop item
            if (hoveredSlot == -1 && cursorItem != null) {
                switch (button) {
                    //drop whole stack
                    case GLFW_MOUSE_BUTTON_1 -> {
                        if (entity != null)
                            entity.dropItem(cursorItem);
                        cursorItem = null;
                    }
                    //drop one item from the stack
                    case GLFW_MOUSE_BUTTON_2 -> {
                        Item item = cursorItem.copy();
                        item.setCount(1);
                        if (entity != null)
                            entity.dropItem(item);
                        cursorItem.setCount(cursorItem.getCount() - 1);
                        if (cursorItem.getCount() <= 0)
                            cursorItem = null;
                    }
                }
                return true;
            }

            //inside bounds, pickup item
            switch (button) {
                //whole stack
                case GLFW_MOUSE_BUTTON_1 -> {
                    Item slotItem = entity.getInventory().getItem(hoveredSlot);

                    //move hotbar-backpack
                    if ((mods & GLFW_MOD_SHIFT) != 0) {
                        if (slotItem != null) {
                            int hotbar = entity.getInventory().getHotbarSize();
                            int totalSize = entity.getInventory().getSize();

                            //move from backpack to hotbar
                            if (hoveredSlot >= hotbar) {
                                if (entity.getInventory().putItem(slotItem, 0, hotbar, false) == 2)
                                    entity.getInventory().setItem(hoveredSlot, null);
                            }
                            //move from hotbar to backpack
                            else {
                                if (entity.getInventory().putItem(slotItem, hotbar, totalSize, false) == 2)
                                    entity.getInventory().setItem(hoveredSlot, null);
                            }
                        }
                    }

                    //swap the cursor item with the hovered slot
                    else if (cursorItem == null || slotItem == null || !slotItem.stacksWith(cursorItem)) {
                        if (hoveredSlot >= 0)
                            entity.getInventory().setItem(hoveredSlot, cursorItem);
                        cursorItem = slotItem;
                    }

                    //append the cursor item to the hovered slot if possible
                    else {
                        int space = slotItem.getStackSize() - slotItem.getCount();
                        if (space > 0) {
                            int toAdd = Math.min(space, cursorItem.getCount());
                            slotItem.setCount(slotItem.getCount() + toAdd);
                            cursorItem.setCount(cursorItem.getCount() - toAdd);
                            if (cursorItem.getCount() <= 0)
                                cursorItem = null;
                        }
                    }
                }
                //single item
                case GLFW_MOUSE_BUTTON_2 -> {
                    Item slotItem = entity.getInventory().getItem(hoveredSlot);

                    //pick up half the stack from the hovered slot if possible
                    if (cursorItem == null) {
                        if (slotItem != null) {
                            int half = (int) Math.ceil(slotItem.getCount() / 2f);
                            cursorItem = slotItem.copy();
                            cursorItem.setCount(half);
                            slotItem.setCount(slotItem.getCount() - half);
                            if (slotItem.getCount() <= 0)
                                entity.getInventory().setItem(hoveredSlot, null);
                        }
                    }

                    //append the cursor item to the empty slot
                    else if (slotItem == null) {
                        Item toAdd = cursorItem.copy();
                        toAdd.setCount(1);
                        entity.getInventory().setItem(hoveredSlot, toAdd);
                        cursorItem.setCount(cursorItem.getCount() - 1);
                        if (cursorItem.getCount() <= 0)
                            cursorItem = null;
                    }

                    //append one item from the cursor to the hovered slot if possible
                    else if (slotItem.stacksWith(cursorItem) && slotItem.getCount() < slotItem.getStackSize()) {
                        slotItem.setCount(slotItem.getCount() + 1);
                        cursorItem.setCount(cursorItem.getCount() - 1);
                        if (cursorItem.getCount() <= 0)
                            cursorItem = null;
                    }
                }
            }

            return true;
        }

        return super.mousePress(button, action, mods);
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action != GLFW_RELEASE) {
            switch (key) {
                case GLFW_KEY_TAB -> {
                    this.close();
                    return true;
                }
                case GLFW_KEY_Q -> {
                    //drop item
                    if (hoveredSlot >= 0 && entity != null) {
                        //ctrl drops the whole stack, otherwise drop one item
                        entity.dropItem(hoveredSlot, (mods & GLFW_MOD_CONTROL) != 0 ? -1 : 1);
                        return true;
                    }
                }
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }
}
