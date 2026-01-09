package cinnamon.world.items;

import cinnamon.model.ModelManager;
import cinnamon.render.MatrixStack;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;

public abstract class Item {

    private final String id;
    protected final int stackSize;
    protected final ModelRenderer model;

    private int count;
    private boolean isFiring, isUsing;
    private LivingEntity source;

    public Item(String id, int count, int stackSize, Resource model) {
        this.id = id;
        this.count = count;
        this.stackSize = stackSize;
        this.model = ModelManager.load(model);
    }

    public abstract Item copy();

    public void tick() {}

    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        matrices.pushMatrix();
        matrices.scale(context.scale);
        model.render(matrices);
        matrices.popMatrix();
    }

    public void worldRender(MatrixStack matrices, float delta) {}

    public boolean fire() {
        if (count <= 0)
            return false;
        this.isFiring = true;
        return true;
    }

    public void stopFiring() {
        this.isFiring = false;
    }

    public boolean use() {
        if (count <= 0)
            return false;
        this.isUsing = true;
        return true;
    }

    public void stopUsing() {
        this.isUsing = false;
    }

    public void select(LivingEntity source) {
        this.source = source;
    }

    public void unselect() {
        stopFiring();
        stopUsing();
        source = null;
    }

    public String getId() {
        return id;
    }

    public int getStackSize() {
        return stackSize;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isUsing() {
        return isUsing;
    }

    public boolean isFiring() {
        return isFiring;
    }

    protected LivingEntity getSource() {
        return source;
    }

    public Object getCountText() {
        return count;
    }

    public ModelRenderer getModel() {
        return model;
    }

    public boolean stacksWith(Item other) {
        return other.getClass() == this.getClass();
    }

    public boolean isStackFull() {
        return count >= stackSize;
    }
}
