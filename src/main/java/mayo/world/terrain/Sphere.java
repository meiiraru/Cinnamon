package mayo.world.terrain;

import mayo.model.ModelRegistry;
import mayo.world.World;

public class Sphere extends Terrain {

    public Sphere(World world) {
        super(ModelRegistry.Terrain.SPHERE.model, world);
    }
}
