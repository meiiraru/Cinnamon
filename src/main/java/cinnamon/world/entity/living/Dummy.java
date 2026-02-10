package cinnamon.world.entity.living;

import cinnamon.gui.DebugScreen;
import cinnamon.registry.EntityRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.render.Camera;
import cinnamon.world.DamageType;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import org.joml.Vector3f;

import java.util.UUID;

public class Dummy extends LivingEntity {

    private static final int MAX_HEALTH = 2147483647;

    public Dummy(UUID uuid) {
        super(uuid, LivingModelRegistry.DUMMY.resource, LivingModelRegistry.DUMMY.eyeHeight, MAX_HEALTH, 1);
    }

    @Override
    protected void collide(PhysEntity entity, CollisionResult result, Vector3f toMove) {
        //do nothing
        //super.collide(entity, result, toMove);
    }

    @Override
    public boolean shouldRenderText(Camera camera) {
        return super.shouldRenderText(camera) && DebugScreen.isTabOpen(DebugScreen.Tab.ENTITIES);
    }

    @Override
    public boolean heal(int amount) {
        boolean sup = super.heal(amount);
        this.setHealth(MAX_HEALTH);
        return sup;
    }

    @Override
    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        boolean sup = super.damage(source, type, amount, crit);
        this.setHealth(MAX_HEALTH);
        return sup;
    }

    @Override
    public void setMotion(float x, float y, float z) {
        super.setMotion(0f, y, 0f);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.DUMMY;
    }
}
