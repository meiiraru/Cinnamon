package mayo.world.items.weapons;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Potato;
import mayo.world.entity.projectile.Projectile;

public class PotatoCannon extends Firearm {

    private static final String ID = "Potato Cannon";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/potato_cannon/potato_cannon.obj"));

    public PotatoCannon(int maxRounds, int reloadTime, int useCooldown) {
        super(ID, MODEL, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(World world, Entity entity) {
        return new Potato(world, entity);
    }
}
