package mayo.world.entity.vehicle;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class Cart extends Vehicle {

    private static final Model MODEL = ModelManager.load(new Resource("models/entities/vehicle/cart/cart.obj"));

    public Cart(World world) {
        super(MODEL, world, 1);
    }

    @Override
    public float getSeatHeight() {
        return 0.4f;
    }

    @Override
    protected float getMoveSpeed() {
        return 0.08f;
    }

    @Override
    protected void motionFallout() {
        this.motion.mul(0.9f, 1f, 0.9f);
    }
}
