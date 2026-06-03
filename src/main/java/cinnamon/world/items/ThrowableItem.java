package cinnamon.world.items;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.world.World;
import org.joml.Math;
import org.joml.Vector3f;

public abstract class ThrowableItem extends Item {

    protected final float minForce, maxForce;
    protected final int maxHeldTicks;

    protected int heldTicks = 0;

    public ThrowableItem(String id, int count, int stackSize, Resource model, float minForce, float maxForce, int maxHeldTicks) {
        super(id, count, stackSize, model);
        this.minForce = minForce;
        this.maxForce = maxForce;
        this.maxHeldTicks = maxHeldTicks;
    }

    @Override
    public void tick() {
        super.tick();

        if (isUsing())
            heldTicks++;
    }

    @Override
    public boolean fire() {
        if (!super.fire())
            return false;

        shoot(minForce);
        return true;
    }

    @Override
    public void stopUsing() {
        boolean wasUsing = isUsing();
        super.stopUsing();

        if (wasUsing)
            shoot(getCurrentForce());
    }

    @Override
    public void unselect() {
        super.unselect();
        heldTicks = 0;
    }

    protected float getCurrentForce() {
        return Math.lerp(minForce, maxForce, Math.min(1f, heldTicks / (float) maxHeldTicks));
    }

    protected void shoot(float force) {
        if (getCount() <= 0)
            return;

        heldTicks = 0;
        setCount(getCount() - 1);
        spawnItem(force);
    }

    protected abstract void spawnItem(float force);

    @Override
    public void worldRender(MatrixStack matrices, float delta) {
        super.worldRender(matrices, delta);

        if (!isUsing())
            return;

        LivingEntity src = getSource();
        World world = src.getWorld();

        Vector3f position = src.getHandPos(src.isLeftHanded(), delta);
        Vector3f motion = src.getAimDir(src.isLeftHanded(), delta, 20f).mul(getCurrentForce());
        Vector3f next = new Vector3f();

        float size = 0.4f;
        float remaining = 1f - size;
        float offset = ((Client.getInstance().ticks + delta) * 0.1f) % 1f;

        position.add(motion.x * offset, motion.y * offset, motion.z * offset);
        motion.y -= world.gravity * offset;

        int steps = 32;
        for (int i = 0; i < steps; i++) {
            next.set(position).add(motion.x * size, motion.y * size, motion.z * size);

            VertexConsumer.WORLD_MAIN_EMISSIVE.consume(GeometryHelper.line(matrices, position.x, position.y, position.z, next.x, next.y, next.z, 0.05f, 0xFFFF72AD));

            next.add(motion.x * remaining, motion.y * remaining, motion.z * remaining);
            position.set(next);
            motion.y -= world.gravity;
        }
    }
}
