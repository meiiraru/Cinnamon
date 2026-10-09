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

    protected SeatEntity leftSeat, rightSeat;

    public Bench() {
        super(TerrainModelRegistry.BENCH.resource, TerrainRegistry.BENCH);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (leftSeat != null) leftSeat.remove();
        if (rightSeat != null) rightSeat.remove();
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
        if (left && leftSeat != null)
            left = false;
        else if (!left && rightSeat != null)
            left = true;

        //check if the seat is occupied
        if (left && leftSeat != null || !left && rightSeat != null)
            return true; //void interaction regardless

        //seat the entity
        final boolean isLeftSeat = left;
        SeatEntity seatEntity = new SeatEntity(UUID.randomUUID()) {
            @Override
            public void remove() {
                super.remove();
                if (isLeftSeat) leftSeat = null;
                else rightSeat = null;
            }
        };
        seatEntity.setPos(localHitPos.set(left ? 0.5f : -0.5f, -0.1f, -0.1f).rotate(benchRot).add(benchCenter));
        seatEntity.setRot(benchRot);
        getWorld().addEntity(seatEntity);
        seatEntity.addRider(entity);

        if (left) leftSeat = seatEntity;
        else rightSeat = seatEntity;

        return true;
    }
}
