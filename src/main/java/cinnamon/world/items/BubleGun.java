package cinnamon.world.items;

import cinnamon.model.ModelManager;
import cinnamon.render.Model;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.particle.SoapParticle;
import org.joml.Vector3f;

public class BubleGun extends Item {


    private static final String ID = "Bubble Gun";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/bubble_gun/bubble_gun.obj"));

    public BubleGun(int stackCount) {
        super(ID, stackCount, MODEL);
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        //pos
        SoapParticle particle = new SoapParticle((int) (Math.random() * 400) + 100);
        particle.setEmissive(true);

        Vector3f motion = source.getLookDir();
        particle.setPos(source.getEyePos().add(motion));

        //motion
        motion.mul(0.1f);
        motion.rotateY((float) Math.toRadians(Math.random() * 90f - 45f));
        motion.rotateX((float) Math.toRadians(Math.random() * 90f - 45f));

        motion.y = (float) (Math.random() * 0.05f) + 0.001f;

        if (source instanceof PhysEntity pe)
            motion.add(pe.getMotion());

        particle.setMotion(motion);

        //add
        source.getWorld().addParticle(particle);
    }
}