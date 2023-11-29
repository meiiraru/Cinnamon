package mayo.world.entity.vehicle;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Cart extends Vehicle {

    private static final Model MODEL = ModelManager.load(new Resource("models/entities/vehicle/cart/cart.obj"));

    public Cart() {
        super(MODEL, 1);
    }

    @Override
    public Vector3f getRiderOffset(Entity rider) {
        return new Vector3f(0, 0.4f, 0);
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
