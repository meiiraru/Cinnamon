package cinnamon.world.terrain;

import cinnamon.registry.TerrainRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;

public class Barrier extends Terrain {

    public Barrier() {
        super(TerrainRegistry.BARRIER);
    }

    @Override
    public void render(MatrixStack matrices, float delta) {

    }

    @Override
    protected void updateAABB() {
        aabb = new AABB(pos, pos).expand(1f, 1f, 1f);
        preciseAABB.clear();
        preciseAABB.add(aabb);
    }

    @Override
    public boolean isSelectable(Entity entity) {
        return entity instanceof Player p && p.isGod();
    }
}
