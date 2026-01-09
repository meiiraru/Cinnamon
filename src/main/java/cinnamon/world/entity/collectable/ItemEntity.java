package cinnamon.world.entity.collectable;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.particle.SmokeParticle;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class ItemEntity extends Collectable {

    public static final Resource PICK_UP_SOUND = new Resource("sounds/entity/misc/pickup.ogg");
    public static final int MAX_AGE = Client.TPS * 5 * 60; //5 minutes

    protected final Item item;
    protected final AABB entityAABB = new AABB();

    protected int pickUpDelay = Client.TPS; //1 second
    protected int age = 0;

    public ItemEntity(UUID uuid, Item item) {
        super(uuid, null);
        this.item = item;
        updateAABB();
    }

    @Override
    public void tick() {
        super.tick();

        if (age >= 0 && ++age > MAX_AGE)
            despawn();

        if (pickUpDelay > 0)
            pickUpDelay--;
    }

    @Override
    protected void renderModel(Camera camera, MatrixStack matrices, float delta) {
        //one renderer
        item.render(ItemRenderContext.ENTITY, matrices, delta);

        int count = item.getCount();
        float percentage = count / (float) item.getStackSize();
        if (count > 1) {
            matrices.pushMatrix();

            //two
            matrices.translate(0.1f, 0.1f, 0f);
            item.render(ItemRenderContext.ENTITY, matrices, delta);

            //three
            if (percentage > 0.33f) {
                matrices.translate(-0.2f, 0f, 0f);
                item.render(ItemRenderContext.ENTITY, matrices, delta);
            }

            //four
            if (percentage > 0.66f) {
                matrices.translate(0.1f, 0.1f, 0.05f);
                item.render(ItemRenderContext.ENTITY, matrices, delta);
            }

            matrices.popMatrix();
        }
    }

    @Override
    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        super.renderDebugHitbox(matrices, delta);
        //entity bounding box
        Vector3f min = entityAABB.getMin(), max = entityAABB.getMax();
        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFF00FF00));
    }

    @Override
    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        super.tickEntityCollisions(entityAABB, toMove);
    }

    @Override
    protected void collide(Entity entity, CollisionResult result, Vector3f toMove) {
        if (isRemoved() || pickUpDelay > 0)
            return;

        if (entity instanceof ItemEntity ie) {
            //attempt to merge the stacks
            if (ie.pickUpDelay <= 0 && !this.item.isStackFull() && !ie.item.isStackFull() && ie.item.stacksWith(this.item)) {
                //determine target as the older item
                ItemEntity target, remainder;
                if (this.age > ie.age) {
                    target = this;
                    remainder = ie;
                } else {
                    target = ie;
                    remainder = this;
                }

                //reset age of both to the younger age
                int young = Math.min(target.age, remainder.age);
                target.age = remainder.age = young;

                int totalCount = ie.item.getCount() + this.item.getCount();
                int maxStackSize = this.item.getStackSize();

                //single stack - remove the remainder
                if (totalCount <= maxStackSize) {
                    target.item.setCount(totalCount);
                    remainder.remove();
                }
                //multiple stacks - fill target and leave remainder
                else {
                    target.item.setCount(maxStackSize);
                    remainder.item.setCount(totalCount - maxStackSize);
                }
            }

            return;
        }

        super.collide(entity, result, toMove);
    }

    @Override
    protected boolean onPickUp(Entity entity) {
        if (!(entity instanceof LivingEntity le))
            return false;

        int pick = le.giveItem(item);

        //pickup sound
        if (pick > 0 && !isSilent())
            world.playSound(PICK_UP_SOUND, SoundCategory.ENTITY, le.getPos()).pitch(Maths.range(0.85f, 1.15f));

        //remove only if entirely consumed
        return pick == 2;
    }

    @Override
    protected void updateAABB() {
        this.aabb.set(getPos());

        if (entityAABB != null) {
            this.entityAABB.set(this.aabb);
            this.entityAABB.inflate(0.75f, 0.5f, 0.75f);
        }

        this.aabb.inflate(0.25f);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.ITEM;
    }

    protected void despawn() {
        spawnDespawnParticles();
        remove();
    }

    protected void spawnDespawnParticles() {
        for (int i = 0; i < 5; i++) {
            SmokeParticle particle = new SmokeParticle((int) (Math.random() * 15) + 10, 0xFFFFFFFF);
            particle.setPos(aabb.getRandomPoint());
            world.addParticle(particle);
        }
    }

    public void setPickUpDelay(int pickUpDelay) {
        this.pickUpDelay = pickUpDelay;
    }

    public int getPickUpDelay() {
        return pickUpDelay;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getAge() {
        return age;
    }
}
