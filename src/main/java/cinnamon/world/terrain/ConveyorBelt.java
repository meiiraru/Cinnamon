package cinnamon.world.terrain;

import cinnamon.math.collision.Hit;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.entity.PhysEntity;
import org.joml.Vector3f;

public class ConveyorBelt extends Terrain {

    protected final Vector3f beltMotion = new Vector3f();
    protected float beltSpeed;

    public ConveyorBelt() {
        this(0.15f);
    }

    public ConveyorBelt(float beltSpeed) {
        super(TerrainModelRegistry.CONVEYOR_BELT.resource, TerrainRegistry.CONVEYOR_BELT);
        this.beltSpeed = beltSpeed;
    }

    @Override
    public void calculateBounds() {
        super.calculateBounds();
        updateSpeed();
    }

    protected void updateSpeed() {
        beltMotion.set(0, 0, beltSpeed).rotate(getTransform().getRot());
    }

    public void setBeltSpeed(float beltSpeed) {
        this.beltSpeed = beltSpeed;
        updateSpeed();
    }

    @Override
    public void collideEntity(PhysEntity entity, Hit collision, Vector3f entityMotion, Vector3f entityMove) {
        if (collision.normal().y() > 0.5f)
            entityMotion.add(beltMotion);
        super.collideEntity(entity, collision, entityMotion, entityMove);
    }
}
