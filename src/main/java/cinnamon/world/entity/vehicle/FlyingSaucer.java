package cinnamon.world.entity.vehicle;

import cinnamon.model.material.Material;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.settings.Settings;
import cinnamon.world.entity.Entity;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class FlyingSaucer extends Vehicle {

    public FlyingSaucer(UUID uuid) {
        super(uuid, EntityModelRegistry.FLYING_SAUCER.resource, 1);
        setGravity(0.25f);
        getController().bindVector3D(
                "steering", Settings.left.get(), Settings.right.get(),
                Settings.jump.get(), Settings.sprint.get(),
                Settings.forward.get(), Settings.backward.get(),
                this::impulse
        ).bindVector2D(
                "look", Settings.lookHorizontal.get(), Settings.lookVertical.get(),
                (x, y) -> this.rotate(0f, x, 0f)
        ).bindVector2D(
                "look_2d", Settings.lookLeft.get(), Settings.lookRight.get(), Settings.lookUp.get(), Settings.lookDown.get(),
                (x, y) -> this.rotate(0f, x, 0f)
        );
    }

    @Override
    public void renderTransparent(Camera camera, MatrixStack matrices, float delta) {
        super.renderTransparent(camera, matrices, delta);

        if (model == null)
            return;

        if (isVisible()) {
            //apply model pose
            matrices.pushMatrix();
            matrices.translate(getPos(delta));
            applyModelPose(camera, matrices, delta);

            //render model
            Material mat = model.getMaterials().get("glass");
            model.render(matrices, mat);

            matrices.popMatrix();
        }
    }

    @Override
    protected void applyImpulse() {
        this.motion.add(impulse.mul(0.125f));
        this.impulse.set(0);
    }

    @Override
    protected void motionFallout() {
        this.motion.mul(0.9f);
    }

    @Override
    protected float getMoveSpeed() {
        return 0.2f;
    }

    @Override
    public void impulse(float left, float up, float forwards) {
        if (riding != null)
            return;

        float l = Math.signum(left);
        float f = Math.signum(forwards);

        this.impulse.set(l, 0, -f);

        if (impulse.lengthSquared() > 1)
            impulse.normalize();

        impulse.y = up;

        impulse.mul(getMoveSpeed());

        //move the entity in facing direction
        this.impulse.rotate(transform.getRot());
    }

    @Override
    public boolean addRider(Entity e) {
        boolean added = super.addRider(e);
        setGravity(added ? 0f : 0.25f);
        return added;
    }

    @Override
    protected void removeRider(Entity e) {
        super.removeRider(e);
        if (getRiders().isEmpty())
            setGravity(0.25f);
    }

    @Override
    public Vector3f getRiderOffset(Entity rider) {
        Vector3f vec = new Vector3f(0, 0.35f * getTransform().getScale().y, 0f);
        vec.rotate(transform.getRot());
        return vec;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.FLYING_SAUCER;
    }
}
