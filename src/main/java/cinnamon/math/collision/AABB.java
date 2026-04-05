package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AABB extends Collider<AABB> {

    private float
            minX, minY, minZ,
            maxX, maxY, maxZ;

    public AABB() {
        this(0, 0, 0, 0, 0, 0);
    }

    public AABB(float size) {
        this(0, 0, 0, size, size, size);
    }

    public AABB(Vector3f dimensions) {
        this(0, 0, 0, dimensions.x, dimensions.y, dimensions.z);
    }

    public AABB(Vector3f min, Vector3f max) {
        this(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB(AABB aabb) {
        this.minX = aabb.minX;
        this.minY = aabb.minY;
        this.minZ = aabb.minZ;
        this.maxX = aabb.maxX;
        this.maxY = aabb.maxY;
        this.maxZ = aabb.maxZ;
    }

    public AABB(OBB obb) {
        this.set(obb);
    }

    public AABB(Sphere sphere) {
        this.set(sphere);
    }

    public AABB set(AABB aabb) {
        return this.set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public AABB set(OBB obb) {
        Vector3f center = obb.getCenter();
        Vector3f half = obb.getHalfExtents();
        Vector3f axisX = obb.getAxisX(); Vector3f axisY = obb.getAxisY(); Vector3f axisZ = obb.getAxisZ();

        float axX = axisX.x * half.x; float axY = axisX.y * half.x; float axZ = axisX.z * half.x;
        float ayX = axisY.x * half.y; float ayY = axisY.y * half.y; float ayZ = axisY.z * half.y;
        float azX = axisZ.x * half.z; float azY = axisZ.y * half.z; float azZ = axisZ.z * half.z;

        return this.set(
                center.x - axX - ayX - azX, center.y - axY - ayY - azY, center.z - axZ - ayZ - azZ,
                center.x + axX + ayX + azX, center.y + axY + ayY + azY, center.z + axZ + ayZ + azZ
        );
    }

    public AABB set(Sphere sphere) {
        Vector3f center = sphere.getCenter();
        float r = sphere.getRadius();
        return this.set(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);
    }

    public AABB set(Vector3f position) {
        return this.set(position.x, position.y, position.z, position.x, position.y, position.z);
    }

    public AABB set(Vector3f min, Vector3f max) {
        return this.set(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public AABB set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        if (maxX < minX) {
            float temp = minX; minX = maxX; maxX = temp;
        }
        if (maxY < minY) {
            float temp = minY; minY = maxY; maxY = temp;
        }
        if (maxZ < minZ) {
            float temp = minZ; minZ = maxZ; maxZ = temp;
        }

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

        return this;
    }

    @Override
    public AABB clone() {
        return new AABB(this);
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        float clampedX = Maths.clamp(x, minX, maxX);
        float clampedY = Maths.clamp(y, minY, maxY);
        float clampedZ = Maths.clamp(z, minZ, maxZ);
        return Vector3f.distance(x, y, z, clampedX, clampedY, clampedZ);
    }

    @Override
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
        return out.set(Maths.clamp(x, minX, maxX), Maths.clamp(y, minY, maxY), Maths.clamp(z, minZ, maxZ));
    }

    public boolean containsBox(AABB other) {
        return minX <= other.minX && maxX >= other.maxX &&
               minY <= other.minY && maxY >= other.maxY &&
               minZ <= other.minZ && maxZ >= other.maxZ;
    }

    public boolean intersectsX(float minX, float maxX) {
        return maxX >= this.minX && minX <= this.maxX;
    }

    public boolean intersectsY(float minY, float maxY) {
        return maxY >= this.minY && minY <= this.maxY;
    }

    public boolean intersectsZ(float minZ, float maxZ) {
        return maxZ >= this.minZ && minZ <= this.maxZ;
    }

    @Override
    public boolean intersectsSphere(Sphere sphere) {
        Vector3f center = sphere.getCenter();
        float radius = sphere.getRadius();

        float clampedX = Maths.clamp(center.x, minX, maxX);
        float clampedY = Maths.clamp(center.y, minY, maxY);
        float clampedZ = Maths.clamp(center.z, minZ, maxZ);

        return center.distanceSquared(clampedX, clampedY, clampedZ) <= radius * radius;
    }

    @Override
    public boolean intersectsAABB(AABB other) {
        return intersectsX(other.minX, other.maxX) && intersectsY(other.minY, other.maxY) && intersectsZ(other.minZ, other.maxZ);
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return obb.intersectsAABB(this);
    }

    @Override
    public Hit collideRay(Ray ray) {
        Vector3f dir = ray.getDirection();
        Vector3f origin = ray.getOrigin();

        //calculate the inverse of the ray dir
        float invX = 1f / dir.x;
        float invY = 1f / dir.y;
        float invZ = 1f / dir.z;

        //intersection distances to each plane
        float t1 = (minX - origin.x) * invX; float t2 = (maxX - origin.x) * invX;
        float t3 = (minY - origin.y) * invY; float t4 = (maxY - origin.y) * invY;
        float t5 = (minZ - origin.z) * invZ; float t6 = (maxZ - origin.z) * invZ;

        //find the nearest and farthest intersection distances
        float tMinX = Math.min(t1, t2); float tMaxX = Math.max(t1, t2);
        float tMinY = Math.min(t3, t4); float tMaxY = Math.max(t3, t4);
        float tMinZ = Math.min(t5, t6); float tMaxZ = Math.max(t5, t6);

        //early rejection if the ray will not collide
        if (tMinX > tMaxY || tMinX > tMaxZ || tMinY > tMaxX || tMinY > tMaxZ || tMinZ > tMaxX || tMinZ > tMaxY)
            return null;

        float tNear = Math.max(Math.max(tMinX, tMinY), tMinZ);
        float tFar  = Math.min(Math.min(tMaxX, tMaxY), tMaxZ);

        //check for NaN meaning that no collisions have happened
        if (Float.isNaN(tNear) || Float.isNaN(tFar))
            return null;

        //normalize the intersection times
        float maxDist = ray.getMaxDistance();
        float normNear = tNear / maxDist;
        float normFar  = tFar  / maxDist;

        //reject if the collision time is over the ray length or behind the ray
        if (normFar <= 0 || normNear >= 1)
            return null;

        //calculate collision position
        Vector3f hitPos = origin.fma(tNear, dir, new Vector3f());

        //calculate normal
        Vector3f normal = new Vector3f();
        //determine which axis we hit by checking which plane tNear matched
        if (tNear == tMinX)
            normal.set(dir.x > 0 ? -1 : 1, 0, 0);
        else if (tNear == tMinY)
            normal.set(0, dir.y > 0 ? -1 : 1, 0);
        else
            normal.set(0, 0, dir.z > 0 ? -1 : 1);

        //return the collision result
        return new Hit(hitPos, normal, normNear, normFar, ray, this);
    }

    @Override
    public AABB translate(float x, float y, float z) {
        minX += x;
        maxX += x;
        minY += y;
        maxY += y;
        minZ += z;
        maxZ += z;
        return this;
    }

    public AABB inflate(Vector3f vec) {
        return this.inflate(vec.x, vec.y, vec.z);
    }

    public AABB inflate(float amount) {
        return this.inflate(amount, amount, amount);
    }

    public AABB inflate(float width, float height, float depth) {
        return this.inflate(width, height, depth, width, height, depth);
    }

    public AABB inflate(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.set(
                this.minX - minX,
                this.minY - minY,
                this.minZ - minZ,
                this.maxX + maxX,
                this.maxY + maxY,
                this.maxZ + maxZ
        );
    }

    public AABB expand(Vector3f vec) {
        return this.expand(vec.x, vec.y, vec.z);
    }

    public AABB expand(float x, float y, float z) {
        if (x < 0f) minX += x;
        else maxX += x;

        if (y < 0f) minY += y;
        else maxY += y;

        if (z < 0f) minZ += z;
        else maxZ += z;

        return this;
    }

    public AABB scale(float scale) {
        return this.scale(scale, scale, scale);
    }

    public AABB scale(Vector3f scale) {
        return this.scale(scale.x, scale.y, scale.z);
    }

    public AABB scale(float x, float y, float z) {
        Vector3f center = getCenter();
        return set(
                (minX - center.x) * x + center.x,
                (minY - center.y) * y + center.y,
                (minZ - center.z) * z + center.z,
                (maxX - center.x) * x + center.x,
                (maxY - center.y) * y + center.y,
                (maxZ - center.z) * z + center.z
        );
    }

    public AABB include(Vector3f point) {
        return include(point.x, point.y, point.z);
    }

    public AABB include(float x, float y, float z) {
        if (x < minX) minX = x;
        else if (x > maxX) maxX = x;

        if (y < minY) minY = y;
        else if (y > maxY) maxY = y;

        if (z < minZ) minZ = z;
        else if (z > maxZ) maxZ = z;

        return this;
    }

    public AABB merge(AABB other) {
        return set(
                Math.min(minX, other.minX),
                Math.min(minY, other.minY),
                Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX),
                Math.max(maxY, other.maxY),
                Math.max(maxZ, other.maxZ)
        );
    }

    public float minX() {
        return minX;
    }

    public float minY() {
        return minY;
    }

    public float minZ() {
        return minZ;
    }

    public float maxX() {
        return maxX;
    }

    public float maxY() {
        return maxY;
    }

    public float maxZ() {
        return maxZ;
    }

    public Vector3f getMin() {
        return new Vector3f(minX, minY, minZ);
    }

    public Vector3f getMax() {
        return new Vector3f(maxX, maxY, maxZ);
    }

    public float getMin(int component) {
        return switch (component) {
            case 0 -> minX;
            case 1 -> minY;
            case 2 -> minZ;
            default -> throw new IllegalArgumentException();
        };
    }

    public float getMax(int component) {
        return switch (component) {
            case 0 -> maxX;
            case 1 -> maxY;
            case 2 -> maxZ;
            default -> throw new IllegalArgumentException();
        };
    }

    public float getWidth() {
        return maxX - minX;
    }

    public float getHeight() {
        return maxY - minY;
    }

    public float getDepth() {
        return maxZ - minZ;
    }

    public Vector3f getDimensions() {
        return new Vector3f(getWidth(), getHeight(), getDepth());
    }

    @Override
    public Vector3f getCenter() {
        return new Vector3f(
            (minX + maxX) * 0.5f,
            (minY + maxY) * 0.5f,
            (minZ + maxZ) * 0.5f
        );
    }

    @Override
    public AABB setCenter(float x, float y, float z) {
        float cx = (minX + maxX) * 0.5f;
        float cy = (minY + maxY) * 0.5f;
        float cz = (minZ + maxZ) * 0.5f;

        float dx = x - cx;
        float dy = y - cy;
        float dz = z - cz;

        return this.translate(dx, dy, dz);
    }

    public Vector3f getRandomPoint() {
        Vector3f dimensions = getDimensions();
        return new Vector3f(
                minX + (float) (Math.random() * dimensions.x),
                minY + (float) (Math.random() * dimensions.y),
                minZ + (float) (Math.random() * dimensions.z)
        );
    }

    public float getXOverlap(AABB other) {
        //check if there is no collision on the other axis
        if (!intersectsY(other.minY, other.maxY) || !intersectsZ(other.minZ, other.maxZ))
            return 0;

        if (this.minX <= other.minX)
            return this.minX - other.maxX;

        return this.maxX - other.minX;
    }

    public float getYOverlap(AABB other) {
        //check if there is no collision on the other axis
        if (!intersectsX(other.minX, other.maxX) || !intersectsZ(other.minZ, other.maxZ))
            return 0;

        if (this.minY <= other.minY)
            return this.minY - other.maxY;

        return this.maxY - other.minY;
    }

    public float getZOverlap(AABB other) {
        //check if there is no collision on the other axis
        if (!intersectsX(other.minX, other.maxX) || !intersectsY(other.minY, other.maxY))
            return 0;

        if (this.minZ <= other.minZ)
            return this.minZ - other.maxZ;

        return this.maxZ - other.minZ;
    }

    public Vector3f getOverlap(AABB other) {
        return new Vector3f(getXOverlap(other), getYOverlap(other), getZOverlap(other));
    }

    public AABB rotateX(float angle) {
        if (angle == 0f)
            return this;

        float a = Math.toRadians(angle);
        float sin = Math.sin(a), cos = Math.cos(a);

        return set(
                minX,
                minY * cos - minZ * sin,
                minY * sin + minZ * cos,
                maxX,
                maxY * cos - maxZ * sin,
                maxY * sin + maxZ * cos
        );
    }

    public AABB rotateY(float angle) {
        if (angle == 0f)
            return this;

        float a = Math.toRadians(angle);
        float sin = Math.sin(a), cos = Math.cos(a);

        return set(
                minX * cos + minZ * sin,
                minY,
                -minX * sin + minZ * cos,
                maxX * cos + maxZ * sin,
                maxY,
                -maxX * sin + maxZ * cos
        );
    }

    public AABB rotateZ(float angle) {
        if (angle == 0f)
            return this;

        float a = Math.toRadians(angle);
        float sin = Math.sin(a), cos = Math.cos(a);

        return set(
                minX * cos - minY * sin,
                minX * sin + minY * cos,
                minZ,
                maxX * cos - maxY * sin,
                maxX * sin + maxY * cos,
                maxZ
        );
    }

    public AABB applyMatrix(Matrix4f matrix) {
        int properties = matrix.properties();
        if ((properties & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        //center and half extents
        float cx = (minX + maxX) * 0.5f; float cy = (minY + maxY) * 0.5f; float cz = (minZ + maxZ) * 0.5f;
        float ex = (maxX - minX) * 0.5f; float ey = (maxY - minY) * 0.5f; float ez = (maxZ - minZ) * 0.5f;

        //transform center
        float ncx = Math.fma(matrix.m00(), cx, Math.fma(matrix.m10(), cy, Math.fma(matrix.m20(), cz, matrix.m30())));
        float ncy = Math.fma(matrix.m01(), cx, Math.fma(matrix.m11(), cy, Math.fma(matrix.m21(), cz, matrix.m31())));
        float ncz = Math.fma(matrix.m02(), cx, Math.fma(matrix.m12(), cy, Math.fma(matrix.m22(), cz, matrix.m32())));

        //compute new half extents
        float nex = Math.fma(Math.abs(matrix.m00()), ex, Math.fma(Math.abs(matrix.m10()), ey, Math.abs(matrix.m20()) * ez));
        float ney = Math.fma(Math.abs(matrix.m01()), ex, Math.fma(Math.abs(matrix.m11()), ey, Math.abs(matrix.m21()) * ez));
        float nez = Math.fma(Math.abs(matrix.m02()), ex, Math.fma(Math.abs(matrix.m12()), ey, Math.abs(matrix.m22()) * ez));

        return set(
                ncx - nex,
                ncy - ney,
                ncz - nez,
                ncx + nex,
                ncy + ney,
                ncz + nez
        );
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        float hx = (maxX - minX) * 0.5f;
        float hy = (maxY - minY) * 0.5f;
        float hz = (maxZ - minZ) * 0.5f;

        float cx = minX + hx;
        float cy = minY + hy;
        float cz = minZ + hz;

        float centerProj = cx * axis.x + cy * axis.y + cz * axis.z;
        float r = hx * Math.abs(axis.x) + hy * Math.abs(axis.y) + hz * Math.abs(axis.z);

        minMax[0] = centerProj - r;
        minMax[1] = centerProj + r;
    }

    @Override
    public Collision collideAABB(AABB aabb) {
        float dx = Math.min(this.maxX, aabb.maxX) - Math.max(this.minX, aabb.minX);
        float dy = Math.min(this.maxY, aabb.maxY) - Math.max(this.minY, aabb.minY);
        float dz = Math.min(this.maxZ, aabb.maxZ) - Math.max(this.minZ, aabb.minZ);

        if (dx <= 0 || dy <= 0 || dz <= 0)
            return null;

        //find the axis of minimum penetration
        Vector3f normal = new Vector3f();
        float depth;
        if (dx < dy && dx < dz) {
            depth = dx;
            normal.set(this.minX < aabb.minX ? 1 : -1, 0, 0);
        } else if (dy < dz) {
            depth = dy;
            normal.set(0, this.minY < aabb.minY ? 1 : -1, 0);
        } else {
            depth = dz;
            normal.set(0, 0, this.minZ < aabb.minZ ? 1 : -1);
        }

        return new Collision(normal, depth, this, aabb);
    }

    @Override
    public Collision collideOBB(OBB obb) {
        return SATHelper.SATCollide(this, obb, SATHelper.AABB_AXES, new Vector3f[]{obb.getAxisX(), obb.getAxisY(), obb.getAxisZ()});
    }

    @Override
    public Collision collideSphere(Sphere sphere) {
        Vector3f sCenter = sphere.getCenter();
        float r = sphere.getRadius();

        //find the closest point on the AABB to the sphere center
        float cx = Math.max(minX, Math.min(sCenter.x, maxX));
        float cy = Math.max(minY, Math.min(sCenter.y, maxY));
        float cz = Math.max(minZ, Math.min(sCenter.z, maxZ));

        float dx = sCenter.x - cx;
        float dy = sCenter.y - cy;
        float dz = sCenter.z - cz;
        float distSq = dx * dx + dy * dy + dz * dz;

        //if the distance is bigger than radius, there is no collision
        if (distSq >= r * r)
            return null;

        float dist = Math.sqrt(distSq);
        Vector3f normal = new Vector3f();

        //sphere center is outside the box so the normal points from box to sphere
        if (dist > Maths.KINDA_SMALL_NUMBER) {
            normal.set(dx / dist, dy / dist, dz / dist);
            return new Collision(normal, r - dist, this, sphere);
        }

        //sphere center is inside the box
        //we find the closest face to push the sphere out
        float dl = sCenter.x - minX;
        float dr = maxX - sCenter.x;
        float dt = sCenter.y - minY;
        float db = maxY - sCenter.y;
        float df = sCenter.z - minZ;
        float dk = maxZ - sCenter.z;

        float minDist = Math.min(dl, Math.min(dr, Math.min(dt, Math.min(db, Math.min(df, dk)))));

        if      (minDist == dl) normal.set(-1,  0,  0);
        else if (minDist == dr) normal.set( 1,  0,  0);
        else if (minDist == dt) normal.set( 0, -1,  0);
        else if (minDist == db) normal.set( 0,  1,  0);
        else if (minDist == df) normal.set( 0,  0, -1);
        else                    normal.set( 0,  0,  1);

        return new Collision(normal, r + minDist, this, sphere);
    }

    @Override
    public Hit sweepAABB(AABB aabb, Vector3f movement) {
        float hx = (maxX - minX) * 0.5f;
        float hy = (maxY - minY) * 0.5f;
        float hz = (maxZ - minZ) * 0.5f;

        float cx = minX + hx;
        float cy = minY + hy;
        float cz = minZ + hz;

        AABB sweep = new AABB(aabb).inflate(hx, hy, hz);
        Ray ray = new Ray(cx, cy, cz, movement.x, movement.y, movement.z, movement.length());
        Hit hit = sweep.collideRay(ray);

        return hit == null ? null : hit.setCollider(aabb);
    }

    @Override
    public Hit sweepOBB(OBB obb, Vector3f velocity) {
        return SATHelper.SATSweep(this, obb, SATHelper.AABB_AXES, new Vector3f[]{obb.getAxisX(), obb.getAxisY(), obb.getAxisZ()}, velocity);
    }

    @Override
    public Hit sweepSphere(Sphere sphere, Vector3f velocity) {
        //test the sphere against the AABB with inverted velocity
        Hit hit = sphere.sweepAABB(this, new Vector3f(velocity).negate());
        if (hit == null)
            return null;

        //invert the hit normal and collider then return
        hit.normal().negate();
        hit.ray().invert();
        return hit.setCollider(sphere);
    }

    @Override
    public String toString() {
        return "AABB{minX=" + minX + " minY=" + minY + " minZ=" + minZ + " maxX=" + maxX + " maxY=" + maxY + " maxZ=" + maxZ + "}";
    }
}
