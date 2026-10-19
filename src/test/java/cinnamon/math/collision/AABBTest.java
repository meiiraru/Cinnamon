package cinnamon.math.collision;

import cinnamon.math.collision.shape.AABB;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AABB Collision Tests")
class AABBTest {

    @Test
    @DisplayName("Intersecting Boxes")
    void testOverlappingBoxes() {
        //two overlapping AABBs
        AABB first  = new AABB(0, 0, 0, 2, 2, 2);
        AABB second = new AABB(1, 1, 1, 3, 3, 3);

        //check if both intersects
        assertTrue(first.intersects(second));
        assertTrue(second.intersects(first));
    }

    @Test
    @DisplayName("Separated Boxes")
    void testSeparatedBoxes() {
        //two non-overlapping AABBs
        AABB first  = new AABB(0,     0, 0, 2, 2, 2);
        AABB second = new AABB(2.01f, 0, 0, 4, 2, 2);

        //check if both do not intersect
        assertFalse(first.intersects(second));
        assertFalse(second.intersects(first));
    }

    @Test
    @DisplayName("Touching Boxes")
    void testTouchingBoxes() {
        //two AABBs that touch at one axis
        AABB first  = new AABB(0, 0, 0, 2, 2, 2);
        AABB second = new AABB(2, 0, 0, 4, 2, 2);

        //check if both intersects
        assertTrue(first.intersects(second));
        assertTrue(second.intersects(first));
    }

    @Test
    @DisplayName("No Collision for Touching Boxes")
    void testTouchingBoxesCollision() {
        //two AABBs that touch at one axis
        AABB first  = new AABB(0, 0, 0, 2, 2, 2);
        AABB second = new AABB(2, 0, 0, 4, 2, 2);

        //check that there is no collision
        assertNull(first.collide(second));
    }

    @Test
    @DisplayName("Collision Details for Overlapping Boxes")
    void testOverlappingBoxesCollision() {
        //two overlapping AABBs
        AABB first  = new AABB(0, 0, 0, 4, 4, 4);
        AABB second = new AABB(3, 1, 1, 5, 3, 3);

        //do the collision detection
        Collision collision = first.collide(second);

        //check that the collision is not null and has the expected result
        assertNotNull(collision);
        assertEquals(1.0f, collision.depth());
        assertEquals(new Vector3f(1, 0, 0), collision.normal());
        assertSame(first, collision.shapeA());
        assertSame(second, collision.shapeB());
    }

    @Test
    @DisplayName("Sweeping Box Collision Detection")
    void testSweepingBoxCollision() {
        //separated AABBs
        AABB stationary = new AABB( 0, 0,    0,     2, 2,    2);
        AABB moving     = new AABB(-4, 0.5f, 0.5f, -2, 1.5f, 1.5f);

        //sweep the moving box towards the stationary box
        Hit hit = moving.sweep(stationary, new Vector3f(4, 0, 0));

        //check that the hit is not null and has the expected result
        assertNotNull(hit);
        assertEquals(0.5f, hit.tNear());
        assertEquals(new Vector3f(-1, 0, 0), hit.normal());
        assertEquals(new Vector3f(0, 1, 1), hit.position());
        assertSame(stationary, hit.collider());
    }

    @Test
    @DisplayName("Sweeping Box Moving Away")
    void testSweepingBoxMovingAway() {
        //separated AABBs
        AABB stationary = new AABB( 0, 0,    0,     2, 2,    2);
        AABB moving     = new AABB(-4, 0.5f, 0.5f, -2, 1.5f, 1.5f);

        //sweep the moving box away from the stationary box
        assertNull(moving.sweep(stationary, new Vector3f(-4, 0, 0)));
    }

    @Test
    @DisplayName("Sweeping Box Already Overlapping")
    void testSweepingBoxAlreadyOverlapping() {
        //overlapping AABBs
        AABB stationary = new AABB( 0, 0, 0, 2, 2, 2);
        AABB moving     = new AABB(-1, 0, 0, 1, 2, 2);

        //sweep the moving box towards the stationary box
        Hit hit = moving.sweep(stationary, new Vector3f(4, 0, 0));

        //check that the hit is not null and has the expected result
        assertNotNull(hit);
        assertTrue(hit.tNear() < 0f);
        assertEquals(new Vector3f(-1, 0, 0), hit.normal());
    }

    @Test
    @DisplayName("Sweep with Zero Velocity")
    void testSweepWithZeroVelocity() {
        //overlapping AABBs
        AABB stationary = new AABB( 0, 0, 0, 2, 2, 2);
        AABB moving     = new AABB(-1, 0, 0, 1, 2, 2);

        //sweep the moving box with zero velocity
        assertNull(moving.sweep(stationary, new Vector3f()));
    }
}
