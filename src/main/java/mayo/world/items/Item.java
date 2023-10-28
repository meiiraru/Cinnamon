package mayo.world.items;

import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.shader.Shader;
import mayo.world.entity.Entity;

public abstract class Item {

    private final String id;
    protected final int stackCount;
    protected final Model model;
    protected int count = 1;

    private boolean isUsing, isAttacking;

    public Item(String id, int stackCount, Model model) {
        this.id = id;
        this.stackCount = stackCount;
        this.model = model;
    }

    public void tick() {}

    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        //render model
        Shader.activeShader.applyMatrixStack(matrices);
        model.render();
    }

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
