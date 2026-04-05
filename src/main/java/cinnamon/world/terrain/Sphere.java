package cinnamon.world.terrain;

import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;

public class Sphere extends Terrain {

    public Sphere() {
        super(TerrainModelRegistry.SPHERE.resource, TerrainRegistry.SPHERE);
    }

    @Override
    protected void updateAABB() {
        super.updateAABB();
        preciseCollider.clear();
        preciseCollider.add(new cinnamon.math.collision.Sphere(getPos(), 0.5f).translate(0.5f, 0.5f, 0.5f));
    }
}
