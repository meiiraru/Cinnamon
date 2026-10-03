package cinnamon.world.terrain;

import cinnamon.math.collision.Collider;
import cinnamon.math.collision.shape.AABB;
import cinnamon.math.collision.shape.OBB;
import cinnamon.model.ModelManager;
import cinnamon.model.mesh.Mesh;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.particle.LeafParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Tree extends Terrain {

    public static final int[] LEAF_COLORS = {0xFFE5858C, 0xFFBD516D, 0xFF80436B};
    protected final Mesh hitbox;
    protected int lastLeafTime = 0;

    public Tree() {
        super(TerrainModelRegistry.TREE.resource, TerrainRegistry.CUSTOM);
        this.hitbox = ModelManager.getMesh(TerrainModelRegistry.TREE_HITBOX.resource);
        getCollisionMask().setMask(1, true);
    }

    @Override
    public void tick() {
        super.tick();

        if (--lastLeafTime <= 0) {
            lastLeafTime = 10;
            LeafParticle leaf = new LeafParticle(600, LEAF_COLORS[(int) (Math.random() * LEAF_COLORS.length)]);
            Vector3f pos = getAABB().getRandomPoint(new Vector3f());
            pos.y = getAABB().minY() + getAABB().getHeight() * 0.75f;
            leaf.setPos(pos);
            leaf.setMotion((float) Math.random() * 0.2f - 0.1f, -0.1f, (float) Math.random() * 0.2f - 0.1f);
            leaf.getCollisionMask().setExcludeMask(1, true);
            ((WorldClient) getWorld()).addParticle(leaf);
        }
    }

    @Override
    protected List<Collider<?>> getModelPreciseCollider() {
        List<Collider<?>> colliders = new ArrayList<>();
        for (AABB groupBound : hitbox.getGroupBounds())
            colliders.add(new OBB(groupBound));
        return colliders;
    }
}
