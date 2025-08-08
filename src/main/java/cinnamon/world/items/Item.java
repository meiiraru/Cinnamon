package cinnamon.world.items;

import cinnamon.model.ModelManager;
import cinnamon.render.MatrixStack;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;

public abstract class Item {

    private final String id;
    protected final int stackCount;
    protected final ModelRenderer model;

    private int count;
    private boolean isFiring, isUsing;
    private Entity source;

    public Item(String id, int count, int stackCount, Resource model) {
        this.id = id;
        this.count = count;
        this.stackCount = stackCount;
        this.model = ModelManager.load(model);
    }

    public void tick() {}

    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        //render model
        model.render(matrices);
    }

    public void worldRender(MatrixStack matrices, float delta) {}

    public boolean fire() {
        this.isFiring = true;
        return true;
    }

    public void stopFiring() {
        this.isFiring = false;
    }

    public boolean use() {
        this.isUsing = true;
        return true;
    }

    public void stopUsing() {
        this.isUsing = false;
    }

    public void select(Entity source) {
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

    public int getStackCount() {
        return stackCount;
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

    protected Entity getSource() {
        return source;
    }

    public Object getCountText() {
        return count;
    }
}
