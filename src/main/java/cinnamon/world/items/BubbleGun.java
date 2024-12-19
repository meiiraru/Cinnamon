package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundInstance;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.particle.SoapParticle;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

public class BubbleGun extends Item {

    private static final Resource SHOOT_LOOP_SOUND = new Resource("sounds/item/bubble_gun/shoot_loop.ogg");
    private static final float DISTANCE = 1f;
    private SoundInstance shoot_loop;

    public BubbleGun(int stackCount) {
        super(ItemModelRegistry.BUBBLE_GUN.id, stackCount, ItemModelRegistry.BUBBLE_GUN.resource);
    }

    @Override
    public void tick() {
        super.tick();
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
        if (shoot_loop == null || shoot_loop.isRemoved())
            shoot_loop = source.getWorld().playSound(SHOOT_LOOP_SOUND, SoundCategory.ENTITY, source.getPos()).volume(1f);
    }

    @Override
    public void stopAttacking(Entity source) {
        super.stopAttacking(source);
        if (shoot_loop != null)
            shoot_loop.stop();
    }

    private static Vector3f spawnPos(Entity source) {
        Hit<Terrain> terrain = source.getLookingTerrain(DISTANCE);
        if (terrain != null)
            return terrain.pos();

        return source.getLookDir().mul(DISTANCE).add(source.getEyePos());
    }
}
