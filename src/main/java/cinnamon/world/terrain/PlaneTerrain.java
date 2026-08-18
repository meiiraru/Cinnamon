package cinnamon.world.terrain;

import cinnamon.math.collision.shape.Plane;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.entity.Entity;
import org.joml.Vector3f;

public class PlaneTerrain extends Terrain {

    private final Plane plane = new Plane();

    public PlaneTerrain(Vector3f normal, float d) {
        this(normal.x, normal.y, normal.z, d);
    }

    public PlaneTerrain(float nx, float ny, float nz, float d) {
        super(null, TerrainRegistry.CUSTOM);
        this.plane.set(nx, ny, nz, d);
    }

    @Override
    public void calculateBounds() {
        plane.applyMatrix(getTransform().getMatrix().pos());
        aabb.set(plane.toAABB());
        preciseCollider.clear();
        preciseCollider.add(plane);
        updateTerrainInWorld();
    }

    @Override
    public boolean isSelectable(Entity entity) {
        return false;
    }

    @Override
    public boolean explode(float explosionStrength) {
        return false;
    }
}
