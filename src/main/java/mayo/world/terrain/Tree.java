package mayo.world.terrain;

import mayo.model.ModelRegistry;
import mayo.world.World;

public class Tree extends Terrain {

    public Tree(World world) {
        super(ModelRegistry.Terrain.TREE.model, world);
    }
}
