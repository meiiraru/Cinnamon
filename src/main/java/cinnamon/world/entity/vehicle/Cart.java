package cinnamon.world.entity.vehicle;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Rotation;
import cinnamon.world.entity.Entity;
import cinnamon.world.particle.StarParticle;
import org.joml.Vector3f;

import java.util.UUID;

public class Cart extends Vehicle {

    private boolean isRailed;


    public Cart(UUID uuid) {
        super(uuid, EntityModelRegistry.CART.model, 1);
    }

    @Override
    public void tick() {
        super.tick();

        if (motion.lengthSquared() > 0.01f) {
            for (int i = -1; i < 2; i += 2) {
                StarParticle star = new StarParticle((int) (Math.random() * 5) + 10, ColorUtils.lerpARGBColor(0xFFDDDDDD, 0xFFFFDDAA, (float) Math.random()));
                Vector3f offset = new Vector3f(0.25f * i, 0f, 0f).rotate(Rotation.Y.rotationDeg(-rot.y));
                Vector3f pos = new Vector3f(getPos());
                star.setPos(pos.add(offset));
                star.setEmissive(true);
                getWorld().addParticle(star);
            }
        }
    }

    @Override
    protected void tickPhysics() {
        if (!isRailed)
            super.tickPhysics();
    }

    @Override
    public Vector3f getRiderOffset(Entity rider) {
        return new Vector3f(0, 0.4f, 0);
    }

    @Override
    protected float getMoveSpeed() {
        return 0.08f;
    }

    @Override
    protected void motionFallout() {
        this.motion.mul(0.9f, 1f, 0.9f);
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
}
