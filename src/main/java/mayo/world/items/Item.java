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

    public Item(String id, int stackCount, Model model) {
        this.id = id;
        this.stackCount = stackCount;
        this.model = model;
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        //render model
        Shader.activeShader.setMatrixStack(matrices);
        model.render();
    }

    public void attack(Entity source) {}

    public void use(Entity source) {}

    public String getId() {
        return id;
    }

    public int getStackCount() {
        return stackCount;
    }

    public int getCount() {
        return count;
    }

    public boolean hasAttack() {
        return false;
    }
}
