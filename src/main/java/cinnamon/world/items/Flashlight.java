package cinnamon.world.items;

import cinnamon.lang.LangManager;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Resource;
import cinnamon.world.light.CookieLight;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;

public class Flashlight extends Item {

    private static final Resource FLASHLIGHT_COOKIE = new Resource("textures/environment/light/flashlight_cookie.png");

    private final CookieLight light = (CookieLight) new CookieLight().texture(FLASHLIGHT_COOKIE).falloff(0f, 20f);
    private boolean active = false;

    public Flashlight(int color) {
        super(ItemModelRegistry.FLASHLIGHT.id, 1, 1, ItemModelRegistry.FLASHLIGHT.resource);
        light.color(color);
    }

    @Override
    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        super.render(context, matrices, delta);

        if (context != ItemRenderContext.HUD && getSource() != null && getSource().getWorld().isClientside()) {
            light.pos(getSource().getEyePos(delta));
            light.direction(getSource().getLookDir(delta));
        }
    }

    @Override
    public boolean use() {
        active = !active;
        World world = getSource().getWorld();
        if (world.isClientside()) {
            if (active) ((WorldClient) world).addLight(light);
            else ((WorldClient) world).removeLight(light);
        }
        return super.use();
    }

    @Override
    public void unselect() {
        World world = getSource().getWorld();
        if (world.isClientside())
            ((WorldClient) world).removeLight(light);

        active = false;
        super.unselect();
    }

    public Object getCountText() {
        return LangManager.get(active ? "gui.on" : "gui.off");
    }
}
