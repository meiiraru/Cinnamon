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
        light.getShadowMask().setExcludeMask(2, true);
    }

    @Override
    public boolean use() {
        setActive(!active);
        return super.use();
    }

    @Override
    public void unselect() {
        setActive(false);
        super.unselect();
    }

    private void setActive(boolean active) {
        this.active = active;

        LivingEntity source;
        World world;
        if ((source = getSource()) == null || (world = source.getWorld()) == null || !(world instanceof WorldClient wc))
            return;

        if (active) wc.addLight(light);
        else wc.removeLight(light);
        source.getRenderMask().setMask(2, active);
    }

    public Object getCountText() {
        return LangManager.get(active ? "gui.on" : "gui.off");
    }
}
