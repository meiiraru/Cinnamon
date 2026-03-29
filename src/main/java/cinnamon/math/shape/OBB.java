package cinnamon.math.shape;

import cinnamon.math.Direction;
import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class OBB extends Shape {

    private final Vector3f center = new Vector3f(), halfExtents = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f
            axisX = new Vector3f(1f, 0f, 0f),
            axisY = new Vector3f(0f, 1f, 0f),
            axisZ = new Vector3f(0f, 0f, 1f);
    private boolean rotDirty = false;

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
        this.setCenter(obb.center);
        this.setHalfExtents(obb.halfExtents);
        this.setRotation(obb.rotation);
    }

    public OBB set(Vector3f center, Vector3f halfExtents) {
        return this.set(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z);
    }

    public OBB set(Vector3f center, Vector3f halfExtents, Quaternionf rotation) {
        return this.set(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z, rotation);
    }

    public OBB set(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ) {
        this.setCenter(centerX, centerY, centerZ);
        this.setHalfExtents(halfX, halfY, halfZ);
        return this;
    }

    public OBB set(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ, Quaternionf rotation) {
        this.setCenter(centerX, centerY, centerZ);
        this.setHalfExtents(halfX, halfY, halfZ);
        this.setRotation(rotation);
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
        this.rotDirty = true;
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
        this.rotDirty = true;
        return this;
    }

    public OBB rotateX(float angle) {
        this.rotation.rotateX(Math.toRadians(angle));
        this.rotDirty = true;
        return this;
    }

    public OBB rotateY(float angle) {
        this.rotation.rotateY(Math.toRadians(angle));
        this.rotDirty = true;
        return this;
    }

    public OBB rotateZ(float angle) {
        this.rotation.rotateZ(Math.toRadians(angle));
        this.rotDirty = true;
        return this;
    }

    public OBB applyMatrix(Matrix4f matrix) {
        int properties = matrix.properties();
        if ((properties & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        matrix.transformPosition(center);
        matrix.transformDirection(halfExtents);
        rotation.mul(matrix.getUnnormalizedRotation(new Quaternionf()));
        this.rotDirty = true;
        return this;
    }

    protected void recalculateAxes() {
        if (!rotDirty)
            return;

        axisX.set(1f, 0f, 0f).rotate(rotation);
        axisY.set(0f, 1f, 0f).rotate(rotation);
        axisZ.set(0f, 0f, 1f).rotate(rotation);
        rotDirty = false;
    }

    public Vector3f getAxisX() {
        this.recalculateAxes();
        return axisX;
    }

    public Vector3f getAxisY() {
        this.recalculateAxes();
        return axisY;
    }

    public Vector3f getAxisZ() {
        this.recalculateAxes();
        return axisZ;
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        //move point into OBB space
        float dx = x - center.x;
        float dy = y - center.y;
        float dz = z - center.z;

        //project into the OBB local axis and compare to the half extents
        Vector3f ax = getAxisX(), ay = getAxisY(), az = getAxisZ();

        //check if it is inside if each local coordinate is within the half extent
        return Math.abs(dx * ax.x + dy * ax.y + dz * ax.z) <= halfExtents.x &&
               Math.abs(dx * ay.x + dy * ay.y + dz * ay.z) <= halfExtents.y &&
               Math.abs(dx * az.x + dy * az.y + dz * az.z) <= halfExtents.z;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        //move point into OBB space
        float dx = x - center.x;
        float dy = y - center.y;
        float dz = z - center.z;

        Vector3f ax = getAxisX(), ay = getAxisY(), az = getAxisZ();

        //project point into local space
        float lx = dx * ax.x + dy * ax.y + dz * ax.z;
        float ly = dx * ay.x + dy * ay.y + dz * ay.z;
        float lz = dx * az.x + dy * az.y + dz * az.z;

        //clamp to half-extents
        float cx = Maths.clamp(lx, -halfExtents.x, halfExtents.x);
        float cy = Maths.clamp(ly, -halfExtents.y, halfExtents.y);
        float cz = Maths.clamp(lz, -halfExtents.z, halfExtents.z);

        //calculate its distance
        return Vector3f.distance(lx, ly, lz, cx, cy, cz);
    }

    private static final Vector3f aabbCenter = new Vector3f(), aabbExtends = new Vector3f();
    @Override
    public boolean intersectsAABB(AABB aabb) {
        //convert aabb to obb with identity rotation and use SAT for OBB vs OBB intersection
        aabbExtends.set(aabb.getWidth() * 0.5f, aabb.getHeight() * 0.5f, aabb.getDepth() * 0.5f);
        aabbCenter.set(aabb.minX() + aabbExtends.x, aabb.minY() + aabbExtends.y, aabb.minZ() + aabbExtends.z);
        return intersectsOBBSAT(
                center, halfExtents, getAxisX(), getAxisY(), getAxisZ(),
                aabbCenter, aabbExtends, Direction.EAST.vector, Direction.UP.vector, Direction.SOUTH.vector
        );
    }

    @Override
    public boolean intersectsSphere(Sphere sphere) {
        //move sphere center into OBB space
        Vector3f sphereCenter = sphere.getCenter();
        float dx = sphereCenter.x - center.x;
        float dy = sphereCenter.y - center.y;
        float dz = sphereCenter.z - center.z;

        //project sphere center into local space
        Vector3f ax = getAxisX(), ay = getAxisY(), az = getAxisZ();
        float lx = dx * ax.x + dy * ax.y + dz * ax.z;
        float ly = dx * ay.x + dy * ay.y + dz * ay.z;
        float lz = dx * az.x + dy * az.y + dz * az.z;

        //find the closest point on the OBB to the sphere center (clamped local)
        float cx = Math.max(-halfExtents.x, Math.min(halfExtents.x, lx));
        float cy = Math.max(-halfExtents.y, Math.min(halfExtents.y, ly));
        float cz = Math.max(-halfExtents.z, Math.min(halfExtents.z, lz));

        //check if the distance between local point and clamped point is less than the radius
        float r = sphere.getRadius();
        return Vector3f.distanceSquared(lx, ly, lz, cx, cy, cz) <= r * r;
    }

    @Override
    public boolean intersectsPlane(Plane plane) {
        //get the direction of the plane and the local orientation axes
        Vector3f n = plane.getNormal();
        Vector3f ax = getAxisX(), ay = getAxisY(), az = getAxisZ();

        //project the radius into the plane normal and calculate teh distance from the box center to the plane
        float projectedRadius = halfExtents.x * Math.abs(n.dot(ax)) + halfExtents.y * Math.abs(n.dot(ay)) + halfExtents.z * Math.abs(n.dot(az));
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
            Vector3f cA, Vector3f hA, Vector3f a0, Vector3f a1, Vector3f a2,
            Vector3f cB, Vector3f hB, Vector3f b0, Vector3f b1, Vector3f b2
    ) {
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
    @Override
    public Ray.Hit collideRay(Ray ray) {
        Vector3f dir = ray.getDirection();
        Vector3f origin = ray.getOrigin();
        Vector3f ax = getAxisX(), ay = getAxisY(), az = getAxisZ();

        //transform ray to OBB local space
        float dx = origin.x - center.x;
        float dy = origin.y - center.y;
        float dz = origin.z - center.z;

        //local origin
        float locOrigX = ax.dot(dx, dy, dz);
        float locOrigY = ay.dot(dx, dy, dz);
        float locOrigZ = az.dot(dx, dy, dz);

        //local direction
        float locDirX = ax.dot(dir);
        float locDirY = ay.dot(dir);
        float locDirZ = az.dot(dir);

        //use AABB slab method in local space
        float invX = 1f / (Math.abs(locDirX) < Maths.SMALL_NUMBER ? Maths.SMALL_NUMBER * Math.signum(locDirX) : locDirX);
        float invY = 1f / (Math.abs(locDirY) < Maths.SMALL_NUMBER ? Maths.SMALL_NUMBER * Math.signum(locDirY) : locDirY);
        float invZ = 1f / (Math.abs(locDirZ) < Maths.SMALL_NUMBER ? Maths.SMALL_NUMBER * Math.signum(locDirZ) : locDirZ);

        //intersection distances to each plane
        float t1 = (-halfExtents.x - locOrigX) * invX; float t2 = ( halfExtents.x - locOrigX) * invX; float t3 = (-halfExtents.y - locOrigY) * invY;
        float t4 = ( halfExtents.y - locOrigY) * invY; float t5 = (-halfExtents.z - locOrigZ) * invZ; float t6 = ( halfExtents.z - locOrigZ) * invZ;

        //find the nearest and farthest intersection distances
        float tMinX = Math.min(t1, t2);
        float tMinY = Math.min(t3, t4);
        float tMinZ = Math.min(t5, t6);

        float tNear = Math.max(Math.max(tMinX, tMinY), tMinZ);
        float tFar  = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
        float maxDist = ray.getMaxDistance();

        //behind ray, misses, or too far away
        if (tFar < 0 || tNear > tFar || tNear > maxDist)
            return null;

        //calculate raycast result
        float tHit = Math.max(0, tNear);
        Vector3f hitPos = origin.fma(tHit, dir, new Vector3f());

        Vector3f worldNormal = new Vector3f();
        if (tNear > 0f) {
            //transform the local axis normal back to world space
            if (tNear == tMinX)
                worldNormal.set(ax).mul(locDirX > 0f ? -1f : 1f);
            else if (tNear == tMinY)
                worldNormal.set(ay).mul(locDirY > 0f ? -1f : 1f);
            else
                worldNormal.set(az).mul(locDirZ > 0f ? -1f : 1f);
        } else {
            worldNormal.set(-dir.x, -dir.y, -dir.z);
        }

        return new Ray.Hit(hitPos, worldNormal, tHit, tFar, maxDist, this);
    }

    @Override
    public String toString() {
        return "OBB{cx=" + center.x + " cy=" + center.y + " cz=" + center.z +
                " hx=" + halfExtents.x + " hy=" + halfExtents.y + " hz=" + halfExtents.z +
                " rot=" + rotation + "}";
    }
}
