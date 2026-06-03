package cinnamon.world.items;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.projectile.Brick;
import cinnamon.world.world.World;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class BrickItem extends Item {

    public static final float MIN_FORCE = 0.6f;
    public static final float MAX_FORCE = 1.5f;
    public static final int MAX_HELD_TICKS = 100;

    protected int heldTicks = 0;

    public BrickItem(int count) {
        super(ItemModelRegistry.BRICK.id, count, 3, ItemModelRegistry.BRICK.resource);
    }

    @Override
    public Item copy() {
        return new BrickItem(getCount());
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.WEAPON;
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

        shoot(MIN_FORCE);
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
        return Math.lerp(MIN_FORCE, MAX_FORCE, Math.min(1f, heldTicks / (float) MAX_HELD_TICKS));
    }

    protected void shoot(float force) {
        if (getCount() <= 0)
            return;

        heldTicks = 0;
        setCount(getCount() - 1);
        LivingEntity src = getSource();

        Brick brick = new Brick(UUID.randomUUID(), src.getUUID());
        brick.setPos(src.getHandPos());
        brick.setMotion(src.getAimDir(20f).mul(force));
        src.getWorld().addEntity(brick);
    }

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
