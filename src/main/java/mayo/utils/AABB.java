package mayo.utils;

import org.joml.Vector3f;

public class AABB {

    private float
            minX, minY, minZ,
            maxX, maxY, maxZ;

    public AABB(Vector3f min, Vector3f max) {
        this(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
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

    public void translate(Vector3f vec) {
        this.translate(vec.x, vec.y, vec.z);
    }

    public void translate(float x, float y, float z) {
        minX += x;
        minY += y;
        minZ += z;
        maxX += x;
        maxY += y;
        maxZ += z;
    }

    public void inflate(Vector3f vec) {
        this.inflate(vec.x, vec.y, vec.z);
    }

    public void inflate(float width, float height, float depth) {
        minX -= width;
        minY -= height;
        minZ -= depth;
        maxX += width;
        maxY += height;
        maxZ += depth;
    }

    public Vector3f getMin() {
        return new Vector3f(minX, minY, minZ);
    }

    public Vector3f getMax() {
        return new Vector3f(maxX, maxY, maxZ);
    }
}
