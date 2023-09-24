package mayo.world.entity.collectable;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.living.Player;

public class HealthPack extends Collectable {

    private static final Model MODEL = ModelManager.load(new Resource("models/entities/ramen/ramen.obj"));
    private static final int HEAL = 10;

    public HealthPack(World world) {
        super(MODEL, world);
    }

    @Override
    protected boolean onPickUp(Entity entity) {
        return entity instanceof Player p && p.heal(HEAL);
    }
}
