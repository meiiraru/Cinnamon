package cinnamon.world.entity.misc;

import cinnamon.registry.EntityRegistry;
import cinnamon.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class TriggerArea extends Entity {

    private final Set<Entity> triggeredEntities = new HashSet<>();
    private Consumer<Entity> enterTrigger, stayTrigger, exitTrigger;
    private final float width, height, depth;

    public TriggerArea(UUID uuid, float width, float height, float depth) {
        super(uuid, null);
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
                if (!triggeredEntities.contains(entity)) {
                    //call enter trigger
                    if (enterTrigger != null)
                        enterTrigger.accept(entity);
                    triggeredEntities.add(entity);
                }

                //call stay trigger
                if (stayTrigger != null)
                    stayTrigger.accept(entity);

                //remove from toRemove set
                toRemove.remove(entity);
            }
        }

        //call exit trigger if applicable
        if (exitTrigger != null)
            for (Entity entity : toRemove)
                exitTrigger.accept(entity);

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

    public void setEnterTrigger(Consumer<Entity> enterTrigger) {
        this.enterTrigger = enterTrigger;
    }

    public void setStayTrigger(Consumer<Entity> stayTrigger) {
        this.stayTrigger = stayTrigger;
    }

    public void setExitTrigger(Consumer<Entity> exitTrigger) {
        this.exitTrigger = exitTrigger;
    }
}
