package cinnamon.world.entity.vehicle;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import cinnamon.world.entity.Entity;
import cinnamon.world.light.Light;
import cinnamon.world.light.Spotlight;
import cinnamon.world.particle.StarParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.UUID;

public class Cart extends Car {

    private final Light
            headlight = new Spotlight().angle(40f, 60f).falloff(0f, 10f).intensity(10f).source(getUUID()),
            taillight = new Spotlight().angle(40f, 60f).falloff(0f, 5f).intensity(10f).source(getUUID()).castsShadows(false).color(0xFF5555);

    private boolean isRailed;

    public Cart(UUID uuid) {
        super(uuid, EntityModelRegistry.CART.resource, 1, 10f, 0.0003f, 0.008f, 0.003f, 0.9f);
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClientside() && motion.lengthSquared() > 0.01f) {
            for (int i = -1; i < 2; i += 2) {
                StarParticle star = new StarParticle((int) (Math.random() * 5) + 10, ColorUtils.lerpARGBColor(0xFFDDDDDD, 0xFFFFDDAA, (float) Math.random()));
                Vector3f offset = new Vector3f(0.25f * i, 0f, 0f).rotate(Rotation.Y.rotationDeg(-rot.y));
                Vector3f pos = new Vector3f(getPos());
                star.setPos(pos.add(offset));
                star.setEmissive(true);
                ((WorldClient) getWorld()).addParticle(star);
            }
        }
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
        Vector2f rot = getRot(delta);
        Vector3f dir = Maths.rotToDir(rot.x + 15f, rot.y);

        Vector3f offset = new Vector3f(0f, 0.5f, -0.85f);
        offset.rotate(Rotation.X.rotationDeg(-rot.x));
        offset.rotate(Rotation.Y.rotationDeg(-rot.y));

        //front light
        headlight.pos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
        headlight.direction(dir);

        //back light
        dir.set(Maths.rotToDir(rot.x - 15f, rot.y));

        offset.set(0f, 0.5f, 0.85f);
        offset.rotate(Rotation.X.rotationDeg(-rot.x));
        offset.rotate(Rotation.Y.rotationDeg(-rot.y));

        taillight.pos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
        taillight.direction(-dir.x, -dir.y, -dir.z);
    }

    @Override
    public Vector3f getRiderOffset(Entity rider) {
        Vector3f vec = new Vector3f(0, 0.4f, 0);
        vec.rotate(Rotation.X.rotationDeg(-rot.x));
        vec.rotate(Rotation.Y.rotationDeg(-rot.y));
        return vec;
    }

    @Override
    public void rotateTo(float pitch, float yaw) {
        super.rotateTo(isRailed ? pitch : 0, yaw);
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
        }
    }
}
