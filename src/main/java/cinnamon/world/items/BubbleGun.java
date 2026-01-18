package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.utils.Maths;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.particle.SoapParticle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

public class BubbleGun extends Item {

    private static final float DISTANCE = 1f;

    public BubbleGun() {
        super(ItemModelRegistry.BUBBLE_GUN.id, 1, 1, ItemModelRegistry.BUBBLE_GUN.resource);
    }

    @Override
    public Item copy() {
        return new BubbleGun();
    }

    @Override
    public void tick() {
        if (isFiring())
            shoot(getSource());
        super.tick();
    }

    @Override
    public boolean fire() {
        if (!super.fire())
            return false;
        shoot(getSource());
        return true;
    }

    private static void shoot(LivingEntity source) {
        if (source.getWorld() == null || !source.getWorld().isClientside())
            return;

        //pos
        SoapParticle particle = new SoapParticle((int) (Math.random() * 400) + 100);
        particle.setPos(spawnPos(source));

        //motion
        Vector3f motion = source.getHandDir(false, 1f);
        motion = Maths.spread(motion, 45f, 45f).mul(0.1f);
        motion.y = (float) (Math.random() * 0.05f) + 0.001f;

        motion.add(source.getMotion());

        particle.setMotion(motion);

        //add
        ((WorldClient) source.getWorld()).addParticle(particle);
    }

    private static Vector3f spawnPos(LivingEntity source) {
        Hit<Terrain> terrain = source.raycastHandTerrain(false, 1f, DISTANCE);
        if (terrain != null)
            return terrain.pos();

        return source.getHandDir(false, 1f).mul(DISTANCE).add(source.getHandPos(false, 1f));
    }

    public Object getCountText() {
        return "";
    }
}
