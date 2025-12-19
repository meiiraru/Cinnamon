package cinnamon.world.terrain;

import cinnamon.registry.TerrainRegistry;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;

public class Barrier extends Terrain {

    public Barrier() {
        super(null, TerrainRegistry.BARRIER);
    }

    @Override
    public boolean isSelectable(Entity entity) {
        return entity instanceof Player p && p.getAbilities().godMode() && super.isSelectable(entity);
    }
}
