package cinnamon.world.items;

import cinnamon.lang.LangManager;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.light.CookieLight;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

public class Flashlight extends Item {

    private static final Resource FLASHLIGHT_COOKIE = new Resource("textures/environment/light/flashlight_cookie.png");

    private final CookieLight light = (CookieLight) new CookieLight().texture(FLASHLIGHT_COOKIE).angle(15f, 20f).falloff(0f, 20f);
    private boolean active = false;

    public Flashlight(int color) {
        super(ItemModelRegistry.FLASHLIGHT.id, 1, 1, ItemModelRegistry.FLASHLIGHT.resource);
        light.color(color);
    }

    @Override
    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        super.render(context, matrices, delta);
        if (context == ItemRenderContext.HUD || !active || getSource() == null || WorldRenderer.isShadowRendering())
            return;

        LivingEntity entity = getSource();
        Vector3f pos = entity.getHandPos(false, delta);
        Vector3f dir = entity.getHandDir(false, delta);

        light.pos(pos);
        light.direction(dir);
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
