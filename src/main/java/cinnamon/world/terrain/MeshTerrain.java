package cinnamon.world.terrain;

import cinnamon.math.collision.MeshCollider;
import cinnamon.model.ModelManager;
import cinnamon.registry.TerrainRegistry;
import cinnamon.utils.Resource;
import org.joml.Matrix4f;

public class MeshTerrain extends Terrain {

    private final MeshCollider rawColl;

    public MeshTerrain(Resource model, TerrainRegistry type) {
        super(model, type);
        this.rawColl = new MeshCollider(ModelManager.getMesh(model));
    }

    @Override
    public void calculateBounds() {
        if (rawColl == null) {
            super.calculateBounds();
            return;
        }

        Matrix4f worldMatrix = new Matrix4f().translate(0.5f, 0f, 0.5f).mul(getTransform().getMatrix().pos());
        MeshCollider meshColl = new MeshCollider(rawColl);
        meshColl.applyMatrix(worldMatrix);

        aabb.set(meshColl.toAABB());
        preciseCollider.clear();
        preciseCollider.add(meshColl);

        updateTerrainInWorld();
    }
}
