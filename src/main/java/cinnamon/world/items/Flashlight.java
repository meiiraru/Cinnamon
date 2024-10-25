package cinnamon.world.items;

import cinnamon.model.ModelManager;
import cinnamon.render.MatrixStack;
import cinnamon.render.Model;
import cinnamon.utils.Resource;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import cinnamon.world.entity.Entity;
import cinnamon.world.light.Spotlight;

public class Flashlight extends Item {

    private static final String ID = "Flashlight";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/flashlight/flashlight.obj"));
    private static final int DELAY_TO_USE = 10;

    private final Spotlight light = (Spotlight) new Spotlight().cutOff(25f, 45f).brightness(64);
    private boolean active = false;
    private Entity owner;

    private int delay = 0;

    public Flashlight(int stackCount, int color) {
        super(ID, stackCount, MODEL);
        light.color(color);
    }

    @Override
    public void tick() {
        super.tick();
        if (delay > 0)
            delay--;
    }

    @Override
    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        super.render(context, matrices, delta);

        if (context != ItemRenderContext.HUD && owner != null && owner.getWorld().isClientside()) {
            light.pos(owner.getEyePos(delta));
            light.direction(owner.getLookDir(delta));
        }
    }

    @Override
    public void use(Entity source) {
        super.use(source);
        owner = source;

        if (delay > 0)
            return;

        delay = DELAY_TO_USE;
        active = !active;

        World world = owner.getWorld();
        if (world.isClientside()) {
            if (active) ((WorldClient) world).addLight(light);
            else ((WorldClient) world).removeLight(light);
        }
    }

    @Override
    public void unselect(Entity source) {
        super.unselect(source);

        active = false;

        if (owner == null)
            return;

        World world = owner.getWorld();
        if (world.isClientside())
            ((WorldClient) world).removeLight(light);
    }
}
