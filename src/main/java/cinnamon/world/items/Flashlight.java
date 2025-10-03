package cinnamon.world.items;

import cinnamon.Client;
import cinnamon.lang.LangManager;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.light.CookieLight;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

public class Flashlight extends Item {

    public static final Resource FLASHLIGHT_COOKIE = new Resource("textures/environment/light/flashlight_cookie.png");

    private final CookieLight light = new CookieLight() {
        @Override
        public void calculateLightSpaceMatrix() {
            updateLightToEntity();
            super.calculateLightSpaceMatrix();
        }
    };

    private boolean active = false;

    public Flashlight(int color) {
        super(ItemModelRegistry.FLASHLIGHT.id, 1, 1, ItemModelRegistry.FLASHLIGHT.resource);
        light
                .cookieTexture(FLASHLIGHT_COOKIE)
                .angle(25f, 30f)
                .falloff(0f, 20f)
                .color(color);
    }

    @Override
    public boolean use() {
        setActive(!active);
        return super.use();
    }

    @Override
    public void select(LivingEntity source) {
        super.select(source);
        light.source(source.getUUID());
    }

    @Override
    public void unselect() {
        setActive(false);
        light.source(null);
        super.unselect();
    }

    private void setActive(boolean active) {
        this.active = active;

        LivingEntity source;
        World world;
        if ((source = getSource()) == null || (world = source.getWorld()) == null || !(world instanceof WorldClient wc))
            return;

        if (active) {
            updateLightToEntity();
            wc.addLight(light);
        } else {
            wc.removeLight(light);
        }
    }

    protected void updateLightToEntity() {
        LivingEntity source;
        if ((source = getSource()) == null)
            return;

        float delta = Client.getInstance().timer.partialTick;
        Vector3f pos = source.getHandPos(false, delta);
        Vector3f dir = source.getHandDir(false, delta);
        light.direction(dir);
        float f = 0.25f;
        light.pos(pos.x + dir.x * f, pos.y + dir.y * f, pos.z + dir.z * f);

        Client c = Client.getInstance();
        boolean fp = source == c.camera.getEntity() && ((WorldClient) source.getWorld()).getCameraMode() == 0;
        boolean noHud = c.hideHUD;
        light.glareSize(fp ? 1f : 5f);
        light.glareIntensity(fp && noHud ? 0f : 1f);
    }

    public Object getCountText() {
        return LangManager.get(active ? "gui.on" : "gui.off");
    }
}
