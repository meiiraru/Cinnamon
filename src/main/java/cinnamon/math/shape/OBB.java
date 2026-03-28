package cinnamon.math.shape;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class OBB extends Shape {

    private final Vector3f center = new Vector3f(), halfExtents = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();

    public OBB() {}

    public OBB(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ) {
        this.set(centerX, centerY, centerZ, halfX, halfY, halfZ);
    }

    public OBB(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ, Quaternionf rotation) {
        this.set(centerX, centerY, centerZ, halfX, halfY, halfZ, rotation);
    }

    public OBB(Vector3f center, Vector3f halfExtents) {
        this(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z);
    }

    public OBB(Vector3f center, Vector3f halfExtents, Quaternionf rotation) {
        this(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z, rotation);
    }

    public OBB(OBB obb) {
        this.center.set(obb.center);
        this.halfExtents.set(obb.halfExtents);
        this.rotation.set(obb.rotation);
    }

    public OBB set(Vector3f center, Vector3f halfExtents) {
        return this.set(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z);
    }

    public OBB set(Vector3f center, Vector3f halfExtents, Quaternionf rotation) {
        return this.set(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z, rotation);
    }

    public OBB set(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ) {
        this.center.set(centerX, centerY, centerZ);
        this.halfExtents.set(halfX, halfY, halfZ);
        return this;
    }

    public OBB set(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ, Quaternionf rotation) {
        this.center.set(centerX, centerY, centerZ);
        this.halfExtents.set(halfX, halfY, halfZ);
        this.rotation.set(rotation);
        return this;
    }

    public OBB setCenter(Vector3f center) {
        return this.setCenter(center.x, center.y, center.z);
    }

    public OBB setCenter(float x, float y, float z) {
        this.center.set(x, y, z);
        return this;
    }

    public Vector3f getCenter() {
        return center;
    }

    public OBB setHalfExtents(Vector3f halfExtents) {
        return this.setHalfExtents(halfExtents.x, halfExtents.y, halfExtents.z);
    }

    public OBB setHalfExtents(float x, float y, float z) {
        this.halfExtents.set(x, y, z);
        return this;
    }

    public Vector3f getHalfExtents() {
        return halfExtents;
    }

    public OBB setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        return this;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public OBB translate(Vector3f translation) {
        return this.translate(translation.x, translation.y, translation.z);
    }

    public OBB translate(float x, float y, float z) {
        this.center.add(x, y, z);
        return this;
    }

    public OBB scale(float scale) {
        return this.scale(scale, scale, scale);
    }

    public OBB scale(Vector3f scale) {
        return this.scale(scale.x, scale.y, scale.z);
    }

    public OBB scale(float scaleX, float scaleY, float scaleZ) {
        this.halfExtents.mul(scaleX, scaleY, scaleZ);
        return this;
    }

    public float getWidth() {
        return halfExtents.x * 2f;
    }

    public float getHeight() {
        return halfExtents.y * 2f;
    }

    public float getDepth() {
        return halfExtents.z * 2f;
    }

    public Vector3f getDimensions() {
        return new Vector3f(halfExtents).mul(2f);
    }

    public OBB rotate(Quaternionf rotation) {
        this.rotation.mul(rotation);
        return this;
    }

    public OBB rotateX(float angle) {
        this.rotation.rotateX(Math.toRadians(angle));
        return this;
    }

    public OBB rotateY(float angle) {
        this.rotation.rotateY(Math.toRadians(angle));
        return this;
    }

    public OBB rotateZ(float angle) {
        this.rotation.rotateZ(Math.toRadians(angle));
        return this;
    }

    public OBB applyMatrix(Matrix4f matrix) {
        int properties = matrix.properties();
        if ((properties & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        matrix.transformPosition(center);
        matrix.transformDirection(halfExtents);
        rotation.mul(matrix.getUnnormalizedRotation(new Quaternionf()));
        return this;
    }

    public Vector3f getAxisX() {
        return new Vector3f(1f, 0f, 0f).rotate(rotation);
    }

    public Vector3f getAxisY() {
        return new Vector3f(0f, 1f, 0f).rotate(rotation);
    }

    public Vector3f getAxisZ() {
        return new Vector3f(0f, 0f, 1f).rotate(rotation);
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        //move point into OBB space, then project into the OBB local axis
        Vector3f p = new Vector3f(x - center.x, y - center.y, z - center.z);
        float localX = p.dot(getAxisX());
        float localY = p.dot(getAxisY());
        float localZ = p.dot(getAxisZ());

        //check if it is inside if each local coordinate is within the half extent
        return Math.abs(localX) <= halfExtents.x &&
               Math.abs(localY) <= halfExtents.y &&
               Math.abs(localZ) <= halfExtents.z;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        //move point into OBB space, then project into the OBB local axis
        Vector3f p = new Vector3f(x - center.x, y - center.y, z - center.z);
        float localX = p.dot(getAxisX());
        float localY = p.dot(getAxisY());
        float localZ = p.dot(getAxisZ());

        //clamp to the closest point to the box in local space
        float clampedX = Maths.clamp(localX, -halfExtents.x, halfExtents.x);
        float clampedY = Maths.clamp(localY, -halfExtents.y, halfExtents.y);
        float clampedZ = Maths.clamp(localZ, -halfExtents.z, halfExtents.z);

        //calculate its distance
        return Vector3f.distance(localX, localY, localZ, clampedX, clampedY, clampedZ);
    }

    @Override
    public boolean intersectsAABB(AABB aabb) {
        //convert aabb to obb with identity rotation and use SAT for OBB vs OBB intersection
        Vector3f bbCenter = aabb.getCenter();
        Vector3f bbHalf = aabb.getDimensions().mul(0.5f);
        return intersectsOBBSAT(
                center, halfExtents, getAxisX(), getAxisY(), getAxisZ(),
                bbCenter, bbHalf, new Vector3f(1f, 0f, 0f), new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, 1f)
        );
    }

    @Override
    public boolean intersectsSphere(Sphere sphere) {
        float dist = distanceToPoint(sphere.getX(), sphere.getY(), sphere.getZ());
        return dist <= sphere.getRadius();
    }

    @Override
    public boolean intersectsPlane(Plane plane) {
        //get the direction of the plane and the local orientation axes
        Vector3f n = plane.getNormal();
        Vector3f axisX = getAxisX();
        Vector3f axisY = getAxisY();
        Vector3f axisZ = getAxisZ();

        //project the radius into the plane normal and calculate teh distance from the box center to the plane
        float projectedRadius = halfExtents.x * Math.abs(n.dot(axisX)) + halfExtents.y * Math.abs(n.dot(axisY)) + halfExtents.z * Math.abs(n.dot(axisZ));
        float distance = n.dot(center) + plane.getConstant();
        return Math.abs(distance) <= projectedRadius;
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return intersectsOBBSAT(
                    center,     halfExtents,     getAxisX(),     getAxisY(),     getAxisZ(),
                obb.center, obb.halfExtents, obb.getAxisX(), obb.getAxisY(), obb.getAxisZ()
        );
    }

    public static boolean intersectsOBBSAT(
            Vector3f aCenter, Vector3f aHalf, Vector3f a0, Vector3f a1, Vector3f a2,
            Vector3f bCenter, Vector3f bHalf, Vector3f b0, Vector3f b1, Vector3f b2
    ) {
        //rotation matrix from B basis into A basis
        //r[i][j] = Ai dot Bj
        float[][]    r = new float[3][3];
        float[][] absR = new float[3][3];

        r[0][0] = a0.dot(b0); r[0][1] = a0.dot(b1); r[0][2] = a0.dot(b2);
        r[1][0] = a1.dot(b0); r[1][1] = a1.dot(b1); r[1][2] = a1.dot(b2);
        r[2][0] = a2.dot(b0); r[2][1] = a2.dot(b1); r[2][2] = a2.dot(b2);

        //store absolute value matrix used for conservative radius projections
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                absR[i][j] = Math.abs(r[i][j]) + Maths.EPSILON;
        }

        //translate B center into A, so all tests can share the same t components
        Vector3f tWorld = new Vector3f(bCenter).sub(aCenter);
        float[] t = new float[] { tWorld.dot(a0), tWorld.dot(a1), tWorld.dot(a2) };

        float[] a = new float[] { aHalf.x, aHalf.y, aHalf.z };
        float[] b = new float[] { bHalf.x, bHalf.y, bHalf.z };

        //test A face normals as separating axes
        for (int i = 0; i < 3; i++) {
            float ra = a[i];
            float rb = b[0] * absR[i][0] + b[1] * absR[i][1] + b[2] * absR[i][2];
            if (Math.abs(t[i]) > ra + rb)
                return false;
        }

        //then B
        for (int j = 0; j < 3; j++) {
            float ra = a[0] * absR[0][j] + a[1] * absR[1][j] + a[2] * absR[2][j];
            float rb = b[j];
            float tOnB = t[0] * r[0][j] + t[1] * r[1][j] + t[2] * r[2][j];
            if (Math.abs(tOnB) > ra + rb)
                return false;
        }

        //test the 9 cross axes (Ai x Bj) (edge vs edge separating axes)
        for (int i = 0; i < 3; i++) {
            int i1 = (i + 1) % 3;
            int i2 = (i + 2) % 3;

            for (int j = 0; j < 3; j++) {
                int j1 = (j + 1) % 3;
                int j2 = (j + 2) % 3;

                float ra = a[i1] * absR[i2][j] + a[i2] * absR[i1][j];
                float rb = b[j1] * absR[i][j2] + b[j2] * absR[i][j1];
                float tCross = Math.abs(t[i2] * r[i1][j] - t[i1] * r[i2][j]);

                if (tCross > ra + rb)
                    return false;
            }
        }

        //no separating axis found - there is an overlap
        return true;
    }

    @Override
    public String toString() {
        return "OBB{cx=" + center.x + " cy=" + center.y + " cz=" + center.z +
                " hx=" + halfExtents.x + " hy=" + halfExtents.y + " hz=" + halfExtents.z +
                " rot=" + rotation + "}";
    }
}
