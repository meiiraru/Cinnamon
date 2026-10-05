package cinnamon.world.terrain;

import cinnamon.math.collision.Hit;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.misc.SeatEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class Bench extends Terrain {

    protected boolean leftSeatOccupied, rightSeatOccupied;

    public Bench() {
        super(TerrainModelRegistry.BENCH.resource, TerrainRegistry.BENCH);
    }

    @Override
    public boolean interact(Entity entity, Hit hit) {
        //check if the hit was on the left or right side of the bench
        Vector3f hitPos = hit.position();
        Vector3f benchCenter = getAABB().getCenter();
        Quaternionf benchRot = getTransform().getRot();

        //calculate the local position of the hit relative to the bench center and rotation
        Vector3f localHitPos = new Vector3f(hitPos).sub(benchCenter).rotate(benchRot.conjugate(new Quaternionf()));
        boolean left = localHitPos.x >= 0f;

        //try to swap the seat if the seat is occupied
        if (left && leftSeatOccupied)
            left = false;
        else if (!left && rightSeatOccupied)
            left = true;

        //check if the seat is occupied
        if (left && leftSeatOccupied || !left && rightSeatOccupied)
            return true; //void interaction regardless

        //seat the entity
        final boolean leftSeat = left;
        SeatEntity seatEntity = new SeatEntity(UUID.randomUUID()) {
            @Override
            public void remove() {
                super.remove();
                if (leftSeat) leftSeatOccupied = false;
                else rightSeatOccupied = false;
            }
        };
        seatEntity.setPos(localHitPos.set(left ? 0.5f : -0.5f, -0.1f, -0.1f).rotate(benchRot).add(benchCenter));
        seatEntity.setRot(benchRot);
        getWorld().addEntity(seatEntity);
        seatEntity.addRider(entity);

        if (left) leftSeatOccupied = true;
        else rightSeatOccupied = true;

        return true;
    }
}
