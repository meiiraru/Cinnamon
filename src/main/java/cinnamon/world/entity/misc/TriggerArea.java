package cinnamon.world.entity.misc;

import cinnamon.registry.EntityRegistry;
import cinnamon.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class TriggerArea extends Entity {

    private final Set<Entity> triggeredEntities = new HashSet<>();
    private final Consumer<Entity> onTrigger;
    private final float width, height, depth;
    private boolean oneTime = true;

    public TriggerArea(UUID uuid, Consumer<Entity> onTrigger, float width, float height, float depth) {
        super(uuid, null);
        this.onTrigger = onTrigger;
        this.width = width * 0.5f;
        this.height = height * 0.5f;
        this.depth = depth * 0.5f;
    }

    @Override
    public void tick() {
        super.tick();

        //test collisions
        Set<Entity> toRemove = new HashSet<>(triggeredEntities);
        for (Entity entity : world.getEntities(getAABB())) {
            if (entity != this) {
                //if already triggered, skip
                if (toRemove.remove(entity))
                    continue;

                //add to triggered list and call consumer
                if (oneTime) triggeredEntities.add(entity);
                onTrigger.accept(entity);
            }
        }

        //remove entities that are no longer in the area
        triggeredEntities.removeAll(toRemove);
    }

    @Override
    protected void updateAABB() {
        this.aabb.set(getPos()).inflate(width, height, depth);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.TRIGGER_AREA;
    }

    public void setOneTime(boolean oneTime) {
        this.oneTime = oneTime;
    }

    public boolean isOneTime() {
        return oneTime;
    }
}
