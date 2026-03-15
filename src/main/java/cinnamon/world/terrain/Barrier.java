package cinnamon.world.terrain;

import cinnamon.registry.TerrainRegistry;
import cinnamon.world.Abilities;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;

public class Barrier extends Terrain {

    public Barrier() {
        super(null, TerrainRegistry.BARRIER);
    }

    @Override
    public boolean isSelectable(Entity entity) {
        return entity instanceof Player p && p.getAbilities().get(Abilities.Ability.GOD_MODE) && super.isSelectable(entity);
    }
}
