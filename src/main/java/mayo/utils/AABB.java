package mayo.utils;

import org.joml.Vector3f;

public class AABB {

    private final float epsilon = 0.001f;
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
        return maxX >= other.minX && minX <= other.maxX &&
                maxY >= other.minY && minY <= other.maxY &&
                maxZ >= other.minZ && minZ <= other.maxZ;
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

    public float clipXCollide(AABB other, float x) {
        //check if there is collision on the Y axis
        if (other.maxY <= this.minY || other.minY >= this.maxY)
            return x;

        //check if there is collision on the Z axis
        if (other.maxZ <= this.minZ || other.minZ >= this.maxZ)
            return x;

        //check for collision if the X axis of the current box is bigger
        if (x > 0f && other.maxX <= this.minX) {
            float max = this.minX - other.maxX - this.epsilon;
            if (max < x)
                x = max;
        }

        //check for collision if the X axis of the other box is smaller
        if (x < 0f && other.minX >= this.maxX) {
            float max = this.maxX - other.minX + this.epsilon;
            if (max > x)
                x = max;
        }

        return x;
    }

    public float clipYCollide(AABB other, float y) {
        //check if there is collision on the X axis
        if (other.maxX <= this.minX || other.minX >= this.maxX)
            return y;

        //check if there is collision on the Z axis
        if (other.maxZ <= this.minZ || other.minZ >= this.maxZ)
            return y;

        //check for collision if the Y axis of the current box is bigger
        if (y > 0f && other.maxY <= this.minY) {
            float max = this.minY - other.maxY - this.epsilon;
            if (max < y)
                y = max;
        }

        //check for collision if the Y axis of the other box is bigger
        if (y < 0f && other.minY >= this.maxY) {
            float max = this.maxY - other.minY + this.epsilon;
            if (max > y)
                y = max;
        }

        return y;
    }

    public float clipZCollide(AABB other, float z) {
        //check if there is collision on the X axis
        if (other.maxX <= this.minX || other.minX >= this.maxX)
            return z;

        //check if there is collision on the Y axis
        if (other.maxY <= this.minY || other.minY >= this.maxY)
            return z;

        //check for collision if the Z axis of the current box is bigger
        if (z > 0f && other.maxZ <= this.minZ) {
            float max = this.minZ - other.maxZ - this.epsilon;
            if (max < z)
                z = max;
        }

        //check for collision if the Z axis of the other box is bigger
        if (z < 0f && other.minZ >= this.maxZ) {
            float max = this.maxZ - other.minZ + this.epsilon;
            if (max > z)
                z = max;
        }

        return z;
    }

    public float getXOverlap(AABB other) {
        //check if there is collision on the Y axis
        if (other.maxY <= this.minY || other.minY >= this.maxY)
            return 0;

        //check if there is collision on the Z axis
        if (other.maxZ <= this.minZ || other.minZ >= this.maxZ)
            return 0;

        if (this.minX <= other.minX)
            return this.minX - other.maxX - this.epsilon;

        return this.maxX - other.minX + this.epsilon;
    }

    public float getYOverlap(AABB other) {
        //check if there is collision on the X axis
        if (other.maxX <= this.minX || other.minX >= this.maxX)
            return 0;

        //check if there is collision on the Z axis
        if (other.maxZ <= this.minZ || other.minZ >= this.maxZ)
            return 0;

        if (this.minY <= other.minY)
            return this.minY - other.maxY - this.epsilon;

        return this.maxY - other.minY + this.epsilon;
    }

    public float getZOverlap(AABB other) {
        //check if there is collision on the X axis
        if (other.maxX <= this.minX || other.minX >= this.maxX)
            return 0;

        //check if there is collision on the Y axis
        if (other.maxY <= this.minY || other.minY >= this.maxY)
            return 0;

        if (this.minZ <= other.minZ)
            return this.minZ - other.maxZ - this.epsilon;

        return this.maxZ - other.minZ + this.epsilon;
    }
}
