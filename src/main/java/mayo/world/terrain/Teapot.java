package mayo.world.terrain;

import mayo.Client;
import mayo.model.ModelRegistry;
import mayo.world.particle.ElectroParticle;

public class Teapot extends Terrain {
    @Override
    public void tick() {
        super.tick();

        if (Client.getInstance().ticks % 3 == 0) {
            ElectroParticle e = new ElectroParticle(5, 0xFFCCFFFF);
            e.setPos(getAABB().getRandomPoint());
            getWorld().addParticle(e);
        }
    }

    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.TEAPOT;
    }
}
