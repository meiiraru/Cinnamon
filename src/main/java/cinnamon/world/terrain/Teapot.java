package cinnamon.world.terrain;

import cinnamon.Client;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.particle.LightParticle;

public class Teapot extends Terrain {

    public Teapot() {
        super(TerrainModelRegistry.TEAPOT.resource, TerrainRegistry.TEAPOT);
    }

    @Override
    public void tick() {
        super.tick();

        if (Client.getInstance().ticks % 3 == 0) {
            int color =
                    0xFF << 24 |
                    (int) (Math.random() * 0x88) + 0x77 << 16 |
                    (int) (Math.random() * 0x88) + 0x77 << 8  |
                    (int) (Math.random() * 0x88) + 0x77;

            LightParticle e = new LightParticle(50, color);
            e.setPos(getAABB().getRandomPoint());
            getWorld().addParticle(e);
        }
    }
}
