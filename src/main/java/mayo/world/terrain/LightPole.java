package mayo.world.terrain;

import mayo.Client;
import mayo.registry.TerrainRegistry;
import mayo.world.particle.LightParticle;
import org.joml.Vector3f;

public class LightPole extends Terrain {
    @Override
    public void tick() {
        super.tick();

        //every half second, spawn a new light particle
        if (Client.getInstance().ticks % 10 == 0) {
            LightParticle light = new LightParticle(60, 0xFFFFFFAA);
            Vector3f pos = new Vector3f((float) Math.random(), (float) Math.random() * 0.5f + 2.75f, (float) Math.random());
            pos.add(getPos());
            light.setPos(pos);
            getWorld().addParticle(light);
        }
    }

    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.LIGHT_POLE;
    }
}
