package cinnamon.world.terrain;

import cinnamon.math.collision.Collider;
import cinnamon.math.collision.shape.AABB;
import cinnamon.math.collision.shape.OBB;
import cinnamon.model.ModelManager;
import cinnamon.model.material.Material;
import cinnamon.model.mesh.Mesh;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.CoreShaders;
import cinnamon.render.shader.Shader;
import cinnamon.world.particle.LeafParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Tree extends Terrain {

    public static final int[] LEAF_COLORS = {0xFFE5858C, 0xFFBD516D, 0xFF80436B};
    protected final ModelRenderer leavesModel;
    protected final Mesh hitbox;
    protected int lastLeafTime = 0;

    public Tree() {
        super(TerrainModelRegistry.TREE.resource, TerrainRegistry.TREE);
        this.hitbox = ModelManager.getMesh(TerrainModelRegistry.TREE_HITBOX.resource);
        this.leavesModel = ModelManager.getRenderer(TerrainModelRegistry.TREE_LEAVES.resource);
        getCollisionMask().setMask(1, true);
    }

    @Override
    protected void renderModel(Camera camera, Material material, MatrixStack matrices, float delta) {
        super.renderModel(camera, material, matrices, delta);

        if (leavesModel != null) {
            if (WorldRenderer.isWorldRendering() && !WorldRenderer.isShadowRendering()) {
                Shader prevSh = Shader.activeShader;
                CoreShaders.GBUFFER_WORLD_PBR_WAVE.getShader().use();
                leavesModel.render(matrices, material);
                prevSh.use();
            } else {
                leavesModel.render(matrices, material);
            }
        }
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
