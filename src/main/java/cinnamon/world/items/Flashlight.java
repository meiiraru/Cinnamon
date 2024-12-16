package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.light.Spotlight;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;

public class Flashlight extends Item {

    private static final Resource USE_SOUND = new Resource("sounds/item.flashlight/switch.ogg");

    private static final int DELAY_TO_USE = 10;

    private final Spotlight light = (Spotlight) new Spotlight().cutOff(25f, 45f).brightness(64);
    private boolean active = false;
    private Entity owner;

    private int delay = 0;

    public Flashlight(int stackCount, int color) {
        super(ItemModelRegistry.FLASHLIGHT.id, stackCount, ItemModelRegistry.FLASHLIGHT.resource);
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

        source.getWorld().playSound(USE_SOUND, SoundCategory.ENTITY, source.getPos());

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
