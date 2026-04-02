package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class OBB extends CollisionShape<OBB> {

    private final Vector3f center = new Vector3f(), halfExtents = new Vector3f();
    private final Vector3f
            axisX = new Vector3f(1f, 0f, 0f),
            axisY = new Vector3f(0f, 1f, 0f),
            axisZ = new Vector3f(0f, 0f, 1f);

    public OBB() {
        this(0, 0, 0, 0, 0, 0);
    }

    public OBB(float scalar) {
        this(0, 0, 0, scalar, scalar, scalar);
    }

    public OBB(float halfX, float halfY, float halfZ) {
        this(0, 0, 0, halfX, halfY, halfZ);
    }

    public OBB(Vector3f halfExtents) {
        this(0, 0, 0, halfExtents.x, halfExtents.y, halfExtents.z);
    }

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
        this.set(obb);
    }

    public OBB(AABB aabb) {
        this.set(aabb);
    }

    public OBB(Sphere sphere) {
        this.set(sphere);
    }

    public OBB set(OBB obb) {
        this.setCenter(obb.center);
        this.setHalfExtents(obb.halfExtents);
        axisX.set(obb.axisX);
        axisY.set(obb.axisY);
        axisZ.set(obb.axisZ);
        return this;
    }

    public OBB set(AABB aabb) {
        float hx = aabb.getWidth()  * 0.5f;
        float hy = aabb.getHeight() * 0.5f;
        float hz = aabb.getDepth()  * 0.5f;
        float cx = aabb.minX() + hx;
        float cy = aabb.minY() + hy;
        float cz = aabb.minZ() + hz;
        return this.set(cx, cy, cz, hx, hy, hz);
    }

    public OBB set(Sphere sphere) {
        Vector3f sCenter = sphere.getCenter();
        float r = sphere.getRadius();
        return this.set(sCenter.x, sCenter.y, sCenter.z, r, r, r);
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

    @Override
    public OBB clone() {
        return new OBB(this);
    }

    @Override
    public OBB setCenter(float x, float y, float z) {
        this.center.set(x, y, z);
        return this;
    }

    @Override
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
        this.identityRotation();
        this.rotate(rotation);
        return this;
    }

    @Override
    public OBB translate(float x, float y, float z) {
        this.center.add(x, y, z);
        return this;
    }

    public OBB translateLocal(Vector3f translation) {
        return this.translateLocal(translation.x, translation.y, translation.z);
    }

    public OBB translateLocal(float x, float y, float z) {
        this.center.add(
                axisX.x * x + axisY.x * y + axisZ.x * z,
                axisX.y * x + axisY.y * y + axisZ.y * z,
                axisX.z * x + axisY.z * y + axisZ.z * z
        );
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
        return new Vector3f(halfExtents.x * 2f, halfExtents.y * 2f, halfExtents.z * 2f);
    }

    public OBB rotate(Quaternionf rotation) {
        axisX.rotate(rotation);
        axisY.rotate(rotation);
        axisZ.rotate(rotation);
        return this;
    }

    public OBB rotate(Quaternionf rotation, Vector3f anchor) {
        return this.rotate(rotation, anchor.x, anchor.y, anchor.z);
    }

    public OBB rotate(Quaternionf rotation, float anchorX, float anchorY, float anchorZ) {
        this.center.sub(anchorX, anchorY, anchorZ).rotate(rotation).add(anchorX, anchorY, anchorZ);
        this.rotate(rotation);
        return this;
    }

    public OBB identityRotation() {
        axisX.set(1, 0, 0);
        axisY.set(0, 1, 0);
        axisZ.set(0, 0, 1);
        return this;
    }

    public OBB rotateX(float angle) {
        float rad = Math.toRadians(angle);
        axisX.rotateX(rad);
        axisY.rotateX(rad);
        axisZ.rotateX(rad);
        return this;
    }

    public OBB rotateX(float angle, Vector3f anchor) {
        return this.rotateX(angle, anchor.x, anchor.y, anchor.z);
    }

    public OBB rotateX(float angle, float anchorX, float anchorY, float anchorZ) {
        if (angle == 0f)
            return this;

        float rad = Math.toRadians(angle);
        float sin = Math.sin(rad), cos = Math.cos(rad);

        float dy = center.y - anchorY;
        float dz = center.z - anchorZ;
        center.y = anchorY + dy * cos - dz * sin;
        center.z = anchorZ + dy * sin + dz * cos;

        axisX.rotateX(rad);
        axisY.rotateX(rad);
        axisZ.rotateX(rad);
        return this;
    }

    public OBB rotateY(float angle) {
        float rad = Math.toRadians(angle);
        axisX.rotateY(rad);
        axisY.rotateY(rad);
        axisZ.rotateY(rad);
        return this;
    }

    public OBB rotateY(float angle, Vector3f anchor) {
        return this.rotateY(angle, anchor.x, anchor.y, anchor.z);
    }

    public OBB rotateY(float angle, float anchorX, float anchorY, float anchorZ) {
        if (angle == 0f)
            return this;

        float rad = Math.toRadians(angle);
        float sin = Math.sin(rad), cos = Math.cos(rad);

        float dx = center.x - anchorX;
        float dz = center.z - anchorZ;
        center.x = anchorX + dx * cos + dz * sin;
        center.z = anchorZ - dx * sin + dz * cos;

        axisX.rotateY(rad);
        axisY.rotateY(rad);
        axisZ.rotateY(rad);
        return this;
    }

    public OBB rotateZ(float angle) {
        float rad = Math.toRadians(angle);
        axisX.rotateZ(rad);
        axisY.rotateZ(rad);
        axisZ.rotateZ(rad);
        return this;
    }

    public OBB rotateZ(float angle, Vector3f anchor) {
        return this.rotateZ(angle, anchor.x, anchor.y, anchor.z);
    }

    public OBB rotateZ(float angle, float anchorX, float anchorY, float anchorZ) {
        if (angle == 0f)
            return this;

        float rad = Math.toRadians(angle);
        float sin = Math.sin(rad), cos = Math.cos(rad);

        float dx = center.x - anchorX;
        float dy = center.y - anchorY;
        center.x = anchorX + dx * cos - dy * sin;
        center.y = anchorY + dx * sin + dy * cos;

        axisX.rotateZ(rad);
        axisY.rotateZ(rad);
        axisZ.rotateZ(rad);
        return this;
    }

    public Vector3f getAxisX() {
        return axisX;
    }

    public Vector3f getAxisY() {
        return axisY;
    }

    public Vector3f getAxisZ() {
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

    @Override
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
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

        //return converted back to world space
        return out.set(
                ax.x * cx + ay.x * cy + az.x * cz + center.x,
                ax.y * cx + ay.y * cy + az.y * cz + center.y,
                ax.z * cx + ay.z * cy + az.z * cz + center.z
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
    public boolean intersectsAABB(AABB aabb) {
        float hx = aabb.getWidth() * 0.5f;
        float hy = aabb.getHeight() * 0.5f;
        float hz = aabb.getDepth() * 0.5f;
        float cx = aabb.minX() + hx;
        float cy = aabb.minY() + hy;
        float cz = aabb.minZ() + hz;

        //convert aabb to obb with identity rotation and use SAT for OBB vs OBB intersection
        return SATHelper.intersectsOBBSAT(
                center, halfExtents, getAxisX(), getAxisY(), getAxisZ(),
                new Vector3f(cx, cy, cz), new Vector3f(hx, hy, hz), AABB.AXIS_X, AABB.AXIS_Y, AABB.AXIS_Z
        );
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return SATHelper.intersectsOBBSAT(
                    center,     halfExtents,     getAxisX(),     getAxisY(),     getAxisZ(),
                obb.center, obb.halfExtents, obb.getAxisX(), obb.getAxisY(), obb.getAxisZ()
        );
    }

    @Override
    public Hit collideRay(Ray ray) {
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

        return new Hit(hitPos, worldNormal, tHit, tFar, ray, this);
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        float centerProj = center.dot(axis);
        float r = halfExtents.x * Math.abs(axisX.dot(axis)) + halfExtents.y * Math.abs(axisY.dot(axis)) + halfExtents.z * Math.abs(axisZ.dot(axis));
        minMax[0] = centerProj - r;
        minMax[1] = centerProj + r;
    }

    @Override
    public Collision collideAABB(AABB aabb) {
        Collision col = aabb.collideOBB(this);
        //reverse direction since calculation went from AABB to OBB
        return col != null ? col.invert() : null;
    }

    @Override
    public Collision collideOBB(OBB obb) {
        return SATHelper.SATCollide(this, obb,
                new Vector3f[]{    getAxisX(),     getAxisY(),     getAxisZ()},
                new Vector3f[]{obb.getAxisX(), obb.getAxisY(), obb.getAxisZ()}
        );
    }

    @Override
    public Collision collideSphere(Sphere sphere) {
        Vector3f sCenter = sphere.getCenter();
        float r = sphere.getRadius();

        //transform sphere center to OBB local space
        float tx = sCenter.x - center.x;
        float ty = sCenter.y - center.y;
        float tz = sCenter.z - center.z;

        float lx = tx * axisX.x + ty * axisX.y + tz * axisX.z;
        float ly = tx * axisY.x + ty * axisY.y + tz * axisY.z;
        float lz = tx * axisZ.x + ty * axisZ.y + tz * axisZ.z;

        //find closest point in local space (same as AABB vs Sphere)
        float cx = Math.max(-halfExtents.x, Math.min(lx, halfExtents.x));
        float cy = Math.max(-halfExtents.y, Math.min(ly, halfExtents.y));
        float cz = Math.max(-halfExtents.z, Math.min(lz, halfExtents.z));

        float dx = lx - cx;
        float dy = ly - cy;
        float dz = lz - cz;
        float distSq = dx * dx + dy * dy + dz * dz;

        if (distSq >= r * r)
            return null;

        float dist = Math.sqrt(distSq);
        Vector3f normal = new Vector3f();

        //outside
        if (dist > Maths.KINDA_SMALL_NUMBER) {
            float nx = dx / dist;
            float ny = dy / dist;
            float nz = dz / dist;

            normal.set(
                    nx * axisX.x + ny * axisY.x + nz * axisZ.x,
                    nx * axisX.y + ny * axisY.y + nz * axisZ.y,
                    nx * axisX.z + ny * axisY.z + nz * axisZ.z
            );
            return new Collision(normal, r - dist, this, sphere);
        }

        //inside
        float dl = lx - (-halfExtents.x);
        float dr = halfExtents.x - lx;
        float dt = ly - (-halfExtents.y);
        float db = halfExtents.y - ly;
        float df = lz - (-halfExtents.z);
        float dk = halfExtents.z - lz;

        float minDist = Math.min(dl, Math.min(dr, Math.min(dt, Math.min(db, Math.min(df, dk)))));
        Vector3f localNormal = new Vector3f();

        if      (minDist == dl) localNormal.set(-1,  0,  0);
        else if (minDist == dr) localNormal.set( 1,  0,  0);
        else if (minDist == dt) localNormal.set( 0, -1,  0);
        else if (minDist == db) localNormal.set( 0,  1,  0);
        else if (minDist == df) localNormal.set( 0,  0, -1);
        else                    localNormal.set( 0,  0,  1);

        //transform local axis to world axis
        normal.set(
                localNormal.x * axisX.x + localNormal.y * axisY.x + localNormal.z * axisZ.x,
                localNormal.x * axisX.y + localNormal.y * axisY.y + localNormal.z * axisZ.y,
                localNormal.x * axisX.z + localNormal.y * axisY.z + localNormal.z * axisZ.z
        );

        return new Collision(normal, r + minDist, this, sphere);
    }

    @Override
    public String toString() {
        return "OBB{cx=" + center.x + " cy=" + center.y + " cz=" + center.z +
                " hx=" + halfExtents.x + " hy=" + halfExtents.y + " hz=" + halfExtents.z +
                " ax0=" + axisX.x + " ax1=" + axisX.y + " ax2=" + axisX.z +
                " ay0=" + axisY.x + " ay1=" + axisY.y + " ay2=" + axisY.z +
                " az0=" + axisZ.x + " az1=" + axisZ.y + " az2=" + axisZ.z + "}";
    }
}
