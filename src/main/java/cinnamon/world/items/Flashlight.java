package cinnamon.world.items;

import cinnamon.Client;
import cinnamon.lang.LangManager;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.light.CookieLight;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;

public class Flashlight extends Item {

    private static final Resource FLASHLIGHT_COOKIE = new Resource("textures/environment/light/flashlight_cookie.png");

    private final CookieLight light = new CookieLight() {
        @Override
        public void calculateLightSpaceMatrix() {
            LivingEntity entity = getSource();
            if (entity != null) {
                float delta = Client.getInstance().timer.partialTick;
                pos(entity.getHandPos(false, delta));
                direction(entity.getHandDir(false, delta));
            }
            super.calculateLightSpaceMatrix();
        }
    };

    private boolean active = false;

    public Flashlight(int color) {
        super(ItemModelRegistry.FLASHLIGHT.id, 1, 1, ItemModelRegistry.FLASHLIGHT.resource);
        light
                .texture(FLASHLIGHT_COOKIE)
                .angle(15f, 20f)
                .falloff(0f, 20f)
                .color(color);
        light.getShadowMask().setExcludeMask(1, true);
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
