package cinnamon.utils;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AABB {

    public static final float epsilon = 0.001f;
    private float
            minX, minY, minZ,
            maxX, maxY, maxZ;

    public AABB() {
        this(0, 0, 0, 0, 0, 0);
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

    public AABB set(AABB aabb) {
        return this.set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
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

    public boolean intersects(AABB other) {
        return intersectsX(other) && intersectsY(other) && intersectsZ(other);
    }

    public boolean intersectsX(AABB other) {
        return maxX >= other.minX && minX <= other.maxX;
    }

    public boolean intersectsY(AABB other) {
        return maxY >= other.minY && minY <= other.maxY;
    }

    public boolean intersectsZ(AABB other) {
        return maxZ >= other.minZ && minZ <= other.maxZ;
    }

    public boolean isInside(AABB other) {
        return other.minX >= minX && other.maxX <= maxX &&
               other.minY >= minY && other.maxY <= maxY &&
               other.minZ >= minZ && other.maxZ <= maxZ;
    }

    public boolean isInside(Vector3f point) {
        return this.isInside(point.x, point.y, point.z);
    }

    public boolean isInside(float x, float y, float z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    public AABB translate(Vector3f vec) {
        return this.translate(vec.x, vec.y, vec.z);
    }

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

    public Vector3f getCenter() {
        return new Vector3f(
            (minX + maxX) * 0.5f,
            (minY + maxY) * 0.5f,
            (minZ + maxZ) * 0.5f
        );
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
        if (!intersectsY(other) || !intersectsZ(other))
            return 0;

        if (this.minX <= other.minX)
            return this.minX - other.maxX - epsilon;

        return this.maxX - other.minX + epsilon;
    }

    public float getYOverlap(AABB other) {
        //check if there is no collision on the other axis
        if (!intersectsX(other) || !intersectsZ(other))
            return 0;

        if (this.minY <= other.minY)
            return this.minY - other.maxY - epsilon;

        return this.maxY - other.minY + epsilon;
    }

    public float getZOverlap(AABB other) {
        //check if there is no collision on the other axis
        if (!intersectsX(other) || !intersectsY(other))
            return 0;

        if (this.minZ <= other.minZ)
            return this.minZ - other.maxZ - epsilon;

        return this.maxZ - other.minZ + epsilon;
    }

    public AABB rotateX(float angle) {
        if (angle == 0f)
            return this;

        double a = Math.toRadians(angle);
        double sin = Math.sin(a), cos = Math.cos(a);

        return set(
                minX,
                (float) (minY * cos - minZ * sin),
                (float) (minY * sin + minZ * cos),
                maxX,
                (float) (maxY * cos - maxZ * sin),
                (float) (maxY * sin + maxZ * cos)
        );
    }

    public AABB rotateY(float angle) {
        if (angle == 0f)
            return this;

        double a = Math.toRadians(angle);
        double sin = Math.sin(a), cos = Math.cos(a);

        return set(
                (float) (minX * cos + minZ * sin),
                minY,
                (float) (-minX * sin + minZ * cos),
                (float) (maxX * cos + maxZ * sin),
                maxY,
                (float) (-maxX * sin + maxZ * cos)
        );
    }

    public AABB rotateZ(float angle) {
        if (angle == 0f)
            return this;

        double a = Math.toRadians(angle);
        double sin = Math.sin(a), cos = Math.cos(a);

        return set(
                (float) (minX * cos - minY * sin),
                (float) (minX * sin + minY * cos),
                minZ,
                (float) (maxX * cos - maxY * sin),
                (float) (maxX * sin + maxY * cos),
                maxZ
        );
    }

    public AABB applyMatrix(Matrix4f matrix) {
        Vector3f min = getMin();
        Vector3f max = getMax();

        min.mulPosition(matrix);
        max.mulPosition(matrix);

        return this.set(min, max);
    }

    @Override
    public String toString() {
        return "(minX=" + minX + " minY=" + minY + " minZ=" + minZ + " maxX=" + maxX + " maxY=" + maxY + " maxZ=" + maxZ + ")";
    }
}
