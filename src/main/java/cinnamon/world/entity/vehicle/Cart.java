package cinnamon.world.entity.vehicle;

import cinnamon.math.Maths;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.settings.Settings;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.items.Flashlight;
import cinnamon.world.light.Light;
import cinnamon.world.light.Spotlight;
import cinnamon.world.particle.StarParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class Cart extends Car {

    public static final Resource
            ALARM_UNLOCK_SOUND = new Resource("sounds/entity/vehicle/car/car_alarm_unlock.png"),
            HORN_PRESS_SOUND   = new Resource("sounds/entity/vehicle/car/car_horn_press.ogg"),
            HORN_RELEASE_SOUND = new Resource("sounds/entity/vehicle/car/car_horn_release.ogg");

    private final Light
            headlight = new Spotlight().angle(40f, 60f).falloff(0f, 10f).intensity(0f).source(getUUID()),
            taillight = new Spotlight().angle(40f, 60f).falloff(0f,  5f).intensity(0f).source(getUUID()).color(0xFF5555);

    private boolean isRailed;
    private boolean lights = false;
    private boolean shouldUpdateLights = true;
    private boolean honking;

    public Cart(UUID uuid) {
        super(uuid, EntityModelRegistry.CART.resource, 1, 10f, 0.0003f, 0.008f, 0.003f, 0.9f);
        taillight.setCastShadows(false);
        getController().bindState(
                "honk", Settings.honk.get(),
                honk -> {
                    if (honk && !honking) {
                        honking = true;
                        ((WorldClient) getWorld()).playSound(HORN_PRESS_SOUND, SoundCategory.ENTITY, getTransform().getPos()).distance(16f).maxDistance(32f);
                    } else if (!honk && honking) {
                        honking = false;
                        ((WorldClient) getWorld()).playSound(HORN_RELEASE_SOUND, SoundCategory.ENTITY, getTransform().getPos()).distance(16f).maxDistance(32f);
                    }
                }
        ).bindClick("lights", Settings.lights.get(),
                clicks -> {
                    shouldUpdateLights = false;
                    setLights(!lights);
                }
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (shouldUpdateLights && !getRiders().isEmpty())
            checkLights();

        if (getWorld().isClientside() && motion.lengthSquared() > 0.01f) {
            float yaw = Maths.getYaw(getTransform().getRot());
            float scale = getTransform().getScale().x;
            for (int i = -1; i < 2; i += 2) {
                StarParticle star = new StarParticle((int) (Math.random() * 5) + 10, ColorUtils.lerpARGBColor(0xFFDDDDDD, 0xFFFFDDAA, (float) Math.random()));
                Vector3f offset = new Vector3f(0.25f * i * scale, 0f, 0f).rotateY(Math.toRadians(-yaw));
                Vector3f pos = new Vector3f(transform.getPos());
                star.setPos(pos.add(offset));
                star.setEmissive(true);
                star.setScale(scale);
                ((WorldClient) getWorld()).addParticle(star);
            }
        }
    }

    protected void checkLights() {
        if (!lights && getWorld().isNight())
            setLights(true);
        else if (lights && !getWorld().isNight())
            setLights(false);
    }

    @Override
    protected void tickPhysics() {
        if (!isRailed)
            super.tickPhysics();
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        super.render(camera, matrices, delta);

        if (!riders.isEmpty())
            updateLights(delta);
    }

    protected void updateLights(float delta) {
        Vector3f pos = getPos(delta);
        Quaternionf quat = getRot(delta);
        Vector3f scale = getScale(delta);
        Vector3f dir = new Vector3f(0f, 0f, -1f * scale.z).rotate(new Quaternionf(quat).rotateX(Math.toRadians(-15f)));

        Vector3f offset = new Vector3f(0f, 0.5f, -0.85f);
        offset.mul(scale);
        offset.rotate(quat);

        //front light
        headlight.pos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
        headlight.direction(dir);

        //back light
        dir.set(0f, 0f, -1f * scale.z).rotate(new Quaternionf(quat).rotateX(Math.toRadians(15f)));

        offset.set(0f, 0.5f, 0.85f);
        offset.mul(scale);
        offset.rotate(quat);

        taillight.pos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
        taillight.direction(-dir.x, -dir.y, -dir.z);
    }

    @Override
    public Vector3f getRiderOffset(Entity rider) {
        Vector3f vec = new Vector3f(0, 0.4f * getTransform().getScale().y, 0);
        vec.rotate(transform.getRot());
        return vec;
    }

    @Override
    public void rotateTo(float pitch, float yaw, float roll) {
        super.rotateTo(isRailed ? pitch : 0f, yaw, roll);
    }

    public void setRailed(boolean railed) {
        isRailed = railed;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.CART;
    }

    @Override
    public boolean addRider(Entity e) {
        int riderCount = riders.size();
        boolean success = super.addRider(e);

        if (success && riderCount == 0 && world instanceof WorldClient w) {
            w.addLight(headlight);
            w.addLight(taillight);
        }

        return success;
    }

    @Override
    protected void removeRider(Entity e) {
        super.removeRider(e);
        if (riders.isEmpty() && world instanceof WorldClient w) {
            w.removeLight(headlight);
            w.removeLight(taillight);
            setLights(false);
            shouldUpdateLights = true;
        }
    }

    public void setLights(boolean lights) {
        if (this.lights == lights)
            return;

        this.lights = lights;
        headlight.intensity(lights ? 10f : 0f);
        taillight.intensity(lights ? 10f : 0f);

        ((WorldClient) getWorld()).playSound(lights ? Flashlight.ON_SOUND : Flashlight.OFF_SOUND, SoundCategory.ENTITY, getTransform().getPos()).volume(5f);
    }
}
