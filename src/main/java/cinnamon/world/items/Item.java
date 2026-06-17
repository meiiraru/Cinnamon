package cinnamon.world.items;

import cinnamon.animation.Animation;
import cinnamon.model.ModelManager;
import cinnamon.render.MatrixStack;
import cinnamon.render.model.AnimatedObjRenderer;
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

    public abstract ItemCategory getCategory();

    public void tick() {}

    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        matrices.pushMatrix();
        matrices.scale(context.scale);
        model.render(matrices);
        matrices.popMatrix();
    }

    public Animation getAnimation(String animation) {
        return model instanceof AnimatedObjRenderer anim ? anim.getAnimation(animation) : null;
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
        this.isFiring = false;
        this.isUsing = false;
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

    public int mergeItem(Item other) {
        if (!stacksWith(other))
            return -1;

        int space = stackSize - count;
        if (space <= 0)
            return -1;

        int otherCount = other.getCount();
        int toAdd = Math.min(space, otherCount);
        count += toAdd;
        other.setCount(otherCount - toAdd);

        return toAdd;
    }
}
