package cinnamon.world.items;

import cinnamon.Client;
import cinnamon.lang.LangManager;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.WorldRenderer;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.light.CookieLight;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

public class Flashlight extends Item {

    public static final Resource
            FLASHLIGHT_COOKIE = new Resource("textures/environment/light/flashlight_cookie.png"),
            ON_SOUND = new Resource("sounds/item/flashlight/on.ogg"),
            OFF_SOUND = new Resource("sounds/item/flashlight/off.ogg");

    private final CookieLight light = new CookieLight() {
        @Override
        public void calculateLightSpaceMatrix() {
            updateLightToEntity();
            super.calculateLightSpaceMatrix();
        }

        @Override
        protected float getDirFallbackAngle() {
            return getSourceYaw();
        }
    };

    private boolean active = false;

    public Flashlight(int color) {
        super(ItemModelRegistry.FLASHLIGHT.id, 1, 1, ItemModelRegistry.FLASHLIGHT.resource);
        light
                .cookieTexture(FLASHLIGHT_COOKIE)
                .angle(15f, 20f)
                .falloff(0f, 20f)
                .color(color);
    }

    @Override
    public Item copy() {
        return new Flashlight(light.getColor());
    }

    @Override
    public boolean use() {
        if (!super.use())
            return false;
        setActive(!active);
        return true;
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
        if (this.active == active)
            return;

        this.active = active;

        LivingEntity source;
        World world;
        if ((source = getSource()) == null || (world = source.getWorld()) == null || !(world instanceof WorldClient wc))
            return;

        if (active) {
            updateLightToEntity();
            wc.addLight(light);
            wc.playSound(ON_SOUND, SoundCategory.ENTITY, source.getPos()).volume(5f);
        } else {
            wc.removeLight(light);
            wc.playSound(OFF_SOUND, SoundCategory.ENTITY, source.getPos()).volume(5f);
        }
    }

    protected void updateLightToEntity() {
        LivingEntity source;
        if ((source = getSource()) == null)
            return;

        Client c = Client.getInstance();

        float delta = source.getWorld().isPaused() ? 1f : c.timer.partialTick;
        Vector3f pos = source.getHandPos(false, delta);
        Vector3f dir = source.getHandDir(false, delta);
        light.direction(dir);
        float f = 0.125f;
        light.pos(pos.x + dir.x * f, pos.y + dir.y * f, pos.z + dir.z * f);

        boolean fp = source == WorldRenderer.camera.getEntity() && !((WorldClient) source.getWorld()).isThirdPerson();
        boolean noHud = c.hideHUD;
        light.glareSize(fp ? 1f : 5f);
        light.glareIntensity(fp && noHud ? 0f : 1f);
    }

    protected float getSourceYaw() {
        LivingEntity source;
        if ((source = getSource()) == null)
            return 0f;
        return source.getRot().y;
    }

    public Object getCountText() {
        return LangManager.get(active ? "gui.on" : "gui.off");
    }
}
