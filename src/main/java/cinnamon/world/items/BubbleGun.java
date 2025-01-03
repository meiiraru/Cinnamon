package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.utils.Maths;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.particle.SoapParticle;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

public class BubbleGun extends Item {

    private static final float DISTANCE = 1f;

    public BubbleGun(int stackCount) {
        super(ItemModelRegistry.BUBBLE_GUN.id, stackCount, ItemModelRegistry.BUBBLE_GUN.resource);
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        //pos
        SoapParticle particle = new SoapParticle((int) (Math.random() * 400) + 100);
        particle.setEmissive(true);
        particle.setPos(spawnPos(source));

        //motion
        Vector3f motion = source.getLookDir();
        motion = Maths.spread(motion, 45f, 45f).mul(0.1f);
        motion.y = (float) (Math.random() * 0.05f) + 0.001f;

        if (source instanceof PhysEntity pe)
            motion.add(pe.getMotion());

        particle.setMotion(motion);

        //add
        source.getWorld().addParticle(particle);
    }

    private static Vector3f spawnPos(Entity source) {
        Hit<Terrain> terrain = source.getLookingTerrain(DISTANCE);
        if (terrain != null)
            return terrain.pos();

        return source.getLookDir().mul(DISTANCE).add(source.getEyePos());
    }
}
