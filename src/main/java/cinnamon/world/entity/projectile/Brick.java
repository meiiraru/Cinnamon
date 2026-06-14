package cinnamon.world.entity.projectile;

import cinnamon.math.Maths;
import cinnamon.math.collision.Hit;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Resource;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.particle.SmokeParticle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class Brick extends Projectile {

    public static final Resource BREAK_SOUND = new Resource("sounds/entity/projectile/brick/break.ogg");

    public Brick(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.BRICK.resource, 8, -1, 1.25f, 0f, owner);
        setGravity(1f);
        setCanSelfDamage(true);
        setRot((float) (Math.random() * 360f), (float) (Math.random() * 360f), (float) (Math.random() * 360f));
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(10, 10, 5);
    }

    @Override
    protected void motionFallout() {
        //no air resistance
    }

    @Override
    protected void collideTerrain(Terrain terrain, Hit hit, Vector3f velocity, Vector3f move) {
        if (!isRemoved())
            remove();
    }

    @Override
    protected boolean canHit(PhysEntity entity, Hit result, Vector3f toMove) {
        boolean sup = super.canHit(entity, result, toMove);
        if (!sup) return false;

        //crit if the hit position is above the entity eye height
        if (result.position().y >= entity.getTransform().getPos().y + entity.getEyeHeight())
            this.crit = true;

        return true;
    }

    @Override
    public void remove() {
        super.remove();

        //sound
        ((WorldClient) getWorld()).playSound(BREAK_SOUND, SoundCategory.ENTITY, getTransform().getPos())
                .pitch(Maths.range(0.85f, 1.15f))
                .maxDistance(16f);

        //particles
        for (int i = 0; i < 5; i++) {
            SmokeParticle particle = new SmokeParticle((int) (Math.random() * 15) + 10, 0xFFA93931);
            particle.setPos(aabb.getRandomPoint());
            ((WorldClient) getWorld()).addParticle(particle);
        }
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.BRICK;
    }
}
