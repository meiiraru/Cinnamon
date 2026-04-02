package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Vector3f;

//Separating Axis Theorem helper class
public class SATHelper {

    public static final Vector3f
            AXIS_X = new Vector3f(1, 0, 0),
            AXIS_Y = new Vector3f(0, 1, 0),
            AXIS_Z = new Vector3f(0, 0, 1);
    public static final Vector3f[] AABB_AXES = {AXIS_X, AXIS_Y, AXIS_Z};

    private static final float[]
            minMaxA = new float[2],
            minMaxB = new float[2];
    private static final Vector3f
            tempNormAxis = new Vector3f(),
            tempCross = new Vector3f(),
            tempDir = new Vector3f();

    public static boolean intersectsOBBSAT(Vector3f cA, Vector3f hA, Vector3f a0, Vector3f a1, Vector3f a2, Vector3f cB, Vector3f hB, Vector3f b0, Vector3f b1, Vector3f b2) {
        //rotation matrix: r_ij = Ai dot Bj
        float r00 = a0.dot(b0), r01 = a0.dot(b1), r02 = a0.dot(b2);
        float r10 = a1.dot(b0), r11 = a1.dot(b1), r12 = a1.dot(b2);
        float r20 = a2.dot(b0), r21 = a2.dot(b1), r22 = a2.dot(b2);

        //common absolute values for radius projections
        float ar00 = Math.abs(r00) + Maths.KINDA_SMALL_NUMBER, ar01 = Math.abs(r01) + Maths.KINDA_SMALL_NUMBER, ar02 = Math.abs(r02) + Maths.KINDA_SMALL_NUMBER;
        float ar10 = Math.abs(r10) + Maths.KINDA_SMALL_NUMBER, ar11 = Math.abs(r11) + Maths.KINDA_SMALL_NUMBER, ar12 = Math.abs(r12) + Maths.KINDA_SMALL_NUMBER;
        float ar20 = Math.abs(r20) + Maths.KINDA_SMALL_NUMBER, ar21 = Math.abs(r21) + Maths.KINDA_SMALL_NUMBER, ar22 = Math.abs(r22) + Maths.KINDA_SMALL_NUMBER;

        //translation vector in world space, then projected into A axes
        float dx = cB.x - cA.x, dy = cB.y - cA.y, dz = cB.z - cA.z;
        float t0 = dx * a0.x + dy * a0.y + dz * a0.z;
        float t1 = dx * a1.x + dy * a1.y + dz * a1.z;
        float t2 = dx * a2.x + dy * a2.y + dz * a2.z;

        //test A axes
        float ra, rb;
        rb = hB.x * ar00 + hB.y * ar01 + hB.z * ar02; if (Math.abs(t0) > hA.x + rb) return false;
        rb = hB.x * ar10 + hB.y * ar11 + hB.z * ar12; if (Math.abs(t1) > hA.y + rb) return false;
        rb = hB.x * ar20 + hB.y * ar21 + hB.z * ar22; if (Math.abs(t2) > hA.z + rb) return false;

        //test B axes
        ra = hA.x * ar00 + hA.y * ar10 + hA.z * ar20; if (Math.abs(t0 * r00 + t1 * r10 + t2 * r20) > ra + hB.x) return false;
        ra = hA.x * ar01 + hA.y * ar11 + hA.z * ar21; if (Math.abs(t0 * r01 + t1 * r11 + t2 * r21) > ra + hB.y) return false;
        ra = hA.x * ar02 + hA.y * ar12 + hA.z * ar22; if (Math.abs(t0 * r02 + t1 * r12 + t2 * r22) > ra + hB.z) return false;

        //test 9 cross-product axes (Ai x Bj)
        ra = hA.y * ar20 + hA.z * ar10; rb = hB.y * ar02 + hB.z * ar01; if (Math.abs(t2 * r10 - t1 * r20) > ra + rb) return false;
        ra = hA.y * ar21 + hA.z * ar11; rb = hB.x * ar02 + hB.z * ar00; if (Math.abs(t2 * r11 - t1 * r21) > ra + rb) return false;
        ra = hA.y * ar22 + hA.z * ar12; rb = hB.x * ar01 + hB.y * ar00; if (Math.abs(t2 * r12 - t1 * r22) > ra + rb) return false;
        ra = hA.x * ar20 + hA.z * ar00; rb = hB.y * ar12 + hB.z * ar11; if (Math.abs(t0 * r20 - t2 * r00) > ra + rb) return false;
        ra = hA.x * ar21 + hA.z * ar01; rb = hB.x * ar12 + hB.z * ar10; if (Math.abs(t0 * r21 - t2 * r01) > ra + rb) return false;
        ra = hA.x * ar22 + hA.z * ar02; rb = hB.x * ar11 + hB.y * ar10; if (Math.abs(t0 * r22 - t2 * r02) > ra + rb) return false;
        ra = hA.x * ar10 + hA.y * ar00; rb = hB.y * ar22 + hB.z * ar21; if (Math.abs(t1 * r00 - t0 * r10) > ra + rb) return false;
        ra = hA.x * ar11 + hA.y * ar01; rb = hB.x * ar22 + hB.z * ar20; if (Math.abs(t1 * r01 - t0 * r11) > ra + rb) return false;
        ra = hA.x * ar12 + hA.y * ar02; rb = hB.x * ar21 + hB.y * ar20; return Math.abs(t1 * r02 - t0 * r12) <= ra + rb; //no axes found a separating plane - overlap
    }

    private static float testAxis(Vector3f axis, Collider<?> a, Collider<?> b) {
        //invalid axis
        if (axis.lengthSquared() < Maths.KINDA_SMALL_NUMBER)
            return Float.MAX_VALUE;

        axis.normalize(tempNormAxis);
        a.project(tempNormAxis, minMaxA);
        b.project(tempNormAxis, minMaxB);

        //returns overlap
        //positive = intersecting, negative = gap
        return Math.min(minMaxA[1], minMaxB[1]) - Math.max(minMaxA[0], minMaxB[0]);
    }

    public static Collision SATCollide(Collider<?> a, Collider<?> b, Vector3f[] axesA, Vector3f[] axesB) {
        float minDepth = Float.MAX_VALUE;
        Vector3f minAxis = new Vector3f();

        //test A axes
        for (Vector3f axis : axesA) {
            float overlap = testAxis(axis, a, b);
            if (overlap <= 0f)
                return null;

            if (overlap < minDepth) {
                minDepth = overlap;
                minAxis.set(axis);
            }
        }

        //test B axes and cross products
        if (axesB != null) {
            for (Vector3f axis : axesB) {
                float overlap = testAxis(axis, a, b);
                if (overlap <= 0f)
                    return null;

                if (overlap < minDepth) {
                    minDepth = overlap;
                    minAxis.set(axis);
                }
            }

            for (Vector3f axisA : axesA) {
                for (Vector3f axisB : axesB) {
                    axisA.cross(axisB, tempCross);
                    float overlap = testAxis(tempCross, a, b);

                    if (overlap <= 0f)
                        return null;

                    if (overlap < minDepth) {
                        minDepth = overlap;
                        minAxis.set(tempCross);
                    }
                }
            }
        }

        //ensure normal points from A to B
        b.getCenter().sub(a.getCenter(), tempDir);
        if (tempDir.dot(minAxis) < 0)
            minAxis.negate();

        return new Collision(minAxis, minDepth, a, b);
    }
}
