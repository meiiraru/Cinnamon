package mayo.utils;

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
        this.set(aabb);
    }

    public AABB set(AABB aabb) {
        this.set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        return this;
    }

    public AABB set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
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
        this.minX -= minX;
        this.minY -= minY;
        this.minZ -= minZ;
        this.maxX += maxX;
        this.maxY += maxY;
        this.maxZ += maxZ;
        return this;
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
}
