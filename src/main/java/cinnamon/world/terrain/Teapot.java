package cinnamon.world.terrain;

import cinnamon.Client;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.particle.ElectroParticle;

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
    public TerrainRegistry getType() {
        return TerrainRegistry.TEAPOT;
    }
}
