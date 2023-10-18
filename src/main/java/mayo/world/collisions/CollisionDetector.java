package mayo.world.collisions;

import mayo.utils.AABB;
import mayo.utils.Maths;
import org.joml.Vector3f;

public class CollisionDetector {
    public static CollisionResult collisionRay(AABB aabb, Vector3f rayPos, Vector3f rayLen) {
        //Calculate intersections with aabb
        Vector3f tNear = aabb.getMin().sub(rayPos).div(rayLen);
        Vector3f tFar = aabb.getMax().sub(rayPos).div(rayLen);

        //check for NaN meaning that no collisions have happened
        if (Maths.isNaN(tNear) || Maths.isNaN(tFar))
            return null;

        //change near and far based on the ray direction
        if (tNear.x > tFar.x) {
            float temp = tNear.x; tNear.x = tFar.x; tFar.x = temp;
        }
        if (tNear.y > tFar.y) {
            float temp = tNear.y; tNear.y = tFar.y; tFar.y = temp;
        }
        if (tNear.z > tFar.z) {
            float temp = tNear.z; tNear.z = tFar.z; tFar.z = temp;
        }

        //early rejection if the ray will not collide
        if (tNear.x > tFar.y || tNear.x > tFar.z || tNear.y > tFar.x || tNear.y > tFar.z || tNear.z > tFar.x || tNear.z > tFar.y)
            return null;

        //grab intersection points time
        float near = Maths.max(tNear);
        float far = Maths.min(tFar);

        //reject if the collision time is over the ray length or behind the ray
        if (far < 0 || near > 1)
            return null;

        //calculate normal
        Vector3f normal = new Vector3f();
        int index = Maths.maxIndex(tNear);
        normal.setComponent(index, rayLen.get(index) <= 0 ? 1 : -1);

        //return the collision result
        return new CollisionResult(near - AABB.epsilon, far - AABB.epsilon, normal);
    }
}
