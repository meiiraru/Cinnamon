package cinnamon.world.entity.terrain;

import cinnamon.registry.TerrainEntityRegistry;
import cinnamon.utils.AABB;
import cinnamon.world.particle.FireParticle;
import cinnamon.world.particle.SoapParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class ParticleSpawner extends TerrainEntity {

    private int bubbles = 0;
    private int fire = 0;

    public ParticleSpawner(UUID uuid) {
        super(uuid, TerrainEntityRegistry.PARTICLE_SPAWNER.resource);
    }

    @Override
    public void tick() {
        super.tick();

        if (!getWorld().isClientside())
            return;

        WorldClient wc = (WorldClient) getWorld();
        if (bubbles > 0) {
            bubbles--;
            SoapParticle p = new SoapParticle((int) (Math.random() * 100) + 100);
            p.setPos(spawnPos());
            p.setMotion((float) (Math.random() * 0.05f) - 0.025f, (float) (Math.random() * 0.05f) + 0.001f, (float) (Math.random() * 0.05f) - 0.025f);
            wc.addParticle(p);
        }

        if (fire > 0) {
            fire--;
            FireParticle p = new FireParticle(20);
            p.setPos(getAABB().getRandomPoint());
            p.setMotion(0, (float) (Math.random() * 0.1f) + 0.1f, 0);
            wc.addParticle(p);
        }
    }

    private Vector3f spawnPos() {
        AABB aabb = getAABB();
        Vector3f center = aabb.getCenter();
        center.y = aabb.maxY();
        return center;
    }

    public void bubbles() {
        bubbles = 25;
    }

    public void fire() {
        fire = 20;
    }
}
