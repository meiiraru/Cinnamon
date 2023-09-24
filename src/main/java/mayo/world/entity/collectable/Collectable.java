package mayo.world.entity.collectable;

import mayo.Client;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.world.World;
import mayo.world.entity.Entity;

public abstract class Collectable extends Entity {

    public Collectable(Model model, World world) {
        super(model, world);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, Client.getInstance().ticks);
    }

    @Override
    protected void renderModel(MatrixStack matrices, float delta) {
        matrices.push();
        matrices.translate(0, ((float) Math.sin((Client.getInstance().ticks + delta) * 0.05f) + 1) * 0.15f, 0);
        super.renderModel(matrices, delta);
        matrices.pop();
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);
        if (!isRemoved() && onPickUp(entity))
            this.removed = true;
    }

    protected abstract boolean onPickUp(Entity entity);
}
