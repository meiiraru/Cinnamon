package mayo.world.terrain;

import mayo.Client;
import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.particle.ElectroParticle;

public class Teapot extends Terrain {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/teapot/teapot.obj"));

    public Teapot(World world) {
        super(MODEL, world);
    }

    @Override
    public void tick() {
        super.tick();

        if (Client.getInstance().ticks % 3 == 0) {
            ElectroParticle e = new ElectroParticle(5, 0xFFCCFFFF);
            e.setPos(getAABB().getRandomPoint());
            getWorld().addParticle(e);
        }
    }
}
