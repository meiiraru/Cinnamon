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
    protected int count = 1;

    private boolean isUsing, isAttacking;

    public Item(String id, int stackCount, Resource model) {
        this.id = id;
        this.stackCount = stackCount;
        this.model = ModelManager.load(model);
    }

    public void tick() {}

    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        //render model
        model.render(matrices);
    }

    public void worldRender(MatrixStack matrices, float delta) {}

    public void attack(Entity source) {
        this.isAttacking = true;
    }

    public void stopAttacking(Entity source) {
        this.isAttacking = false;
    }

    public void use(Entity source) {
        this.isUsing = true;
    }

    public void stopUsing(Entity source) {
        this.isUsing = false;
    }

    public void unselect(Entity source) {
        stopAttacking(source);
        stopUsing(source);
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

    public boolean isUsing() {
        return isUsing;
    }

    public boolean isAttacking() {
        return isAttacking;
    }
}
