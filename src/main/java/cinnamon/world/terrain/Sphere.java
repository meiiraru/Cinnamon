package cinnamon.world.terrain;

import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import org.joml.Matrix4f;

public class Sphere extends Terrain {

    public Sphere() {
        super(TerrainModelRegistry.SPHERE.resource, TerrainRegistry.SPHERE);
    }

    @Override
    public void setScale(float x, float y, float z) {
        float max = Math.max(x, Math.max(y, z));
        super.setScale(max, max, max);
    }

    @Override
    protected void updateAABB() {
        Matrix4f mat = new Matrix4f().translate(0.5f, 0f, 0.5f).mul(transform.getMatrix().pos());
        cinnamon.math.collision.Sphere sphere = new cinnamon.math.collision.Sphere(0f, 0.5f, 0f, 0.5f).applyMatrix(mat);

        this.aabb.set(sphere);
        preciseCollider.clear();
        preciseCollider.add(sphere);

        updateTerrainInWorld();
    }
}
