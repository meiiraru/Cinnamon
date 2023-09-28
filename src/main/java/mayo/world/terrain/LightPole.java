package mayo.world.terrain;

import mayo.Client;
import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.particle.LightParticle;
import org.joml.Vector3f;

public class LightPole extends Terrain {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/light_pole/light_pole.obj"));

    public LightPole(World world) {
        super(MODEL, world);
    }

    @Override
    public void tick() {
        super.tick();

        //every half second, spawn a new light particle
        if (Client.getInstance().ticks % 10 == 0) {
            LightParticle light = new LightParticle(60, 0xFFFFFFAA);
            Vector3f pos = new Vector3f((float) Math.random() - 0.5f, (float) Math.random() * 0.5f + 2.75f, (float) Math.random() - 0.5f);
            pos.add(getPos());
            light.setPos(pos);
            getWorld().addParticle(light);
        }
    }
}
