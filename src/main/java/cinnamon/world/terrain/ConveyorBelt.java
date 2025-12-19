package cinnamon.world.terrain;

import cinnamon.registry.TerrainRegistry;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.world.World;
import org.joml.Vector3f;

import java.util.List;

public class ConveyorBelt extends Terrain {

    protected final AABB beltArea = new AABB();
    protected final Vector3f beltMotion = new Vector3f();
    protected float beltSpeed;

    public ConveyorBelt() {
        this(0.15f);
    }

    public ConveyorBelt(float beltSpeed) {
        super(new Resource("models/terrain/conveyor_belt/model.obj"), TerrainRegistry.CUSTOM);
        this.beltSpeed = beltSpeed;
    }

    @Override
    public void tick() {
        super.tick();

        World w = getWorld();
        List<Entity> entities = w.getEntities(beltArea);
        for (Entity entity : entities) {
            if (entity instanceof PhysEntity pe && pe.isOnGround())
                pe.getMotion().add(beltMotion);
        }
    }

    @Override
    protected void updateAABB() {
        super.updateAABB();

        if (beltArea == null)
            return;

        beltArea.set(
                this.aabb.minX(), this.aabb.maxY(), this.aabb.minZ(),
                this.aabb.maxX(), this.aabb.maxY() + 0.1f, this.aabb.maxZ()
        );

        updateSpeed();
    }

    protected void updateSpeed() {
        beltMotion.set(0, 0, beltSpeed).rotate(Rotation.Y.rotationDeg(getRotationAngle()));
    }

    public void setBeltSpeed(float beltSpeed) {
        this.beltSpeed = beltSpeed;
        updateSpeed();
    }
}
