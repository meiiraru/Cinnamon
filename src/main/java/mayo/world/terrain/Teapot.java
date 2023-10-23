package mayo.world.terrain;

import mayo.Client;
import mayo.model.ModelRegistry;
import mayo.world.World;
import mayo.world.particle.ElectroParticle;

public class Teapot extends Terrain {

    public Teapot(World world) {
        super(ModelRegistry.Terrain.TEAPOT.model, world);
    }

    @Override
    public void tick() {
        super.tick();

        if (Client.getInstance().ticks % 3 == 0) {
            ElectroParticle e = new ElectroParticle(world, 5, 0xFFCCFFFF);
            e.setPos(getAABB().getRandomPoint());
            getWorld().addParticle(e);
        }
    }
}
