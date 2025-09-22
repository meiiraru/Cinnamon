package cinnamon.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Represents the viewing frustum of a camera
 * <p>
 * This class can extract the 6 planes of the frustum from a combined
 * projection-view matrix and perform culling tests against points and AABBs
 */
public class Frustum {

    //left, right, bottom, top, near, far
    private final Vector4f[] planes = {new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()};
    private final Matrix4f matrix = new Matrix4f();

    /**
     * Updates the frustum planes by extracting them from a combined projection-view matrix
     * <p>
     * The planes are normalized after extraction
     * @param viewProj The combined view-projection matrix
     */
    public void update(Matrix4f viewProj) {
        matrix.set(viewProj);

        //grab raw matrix values
        float m00 = matrix.m00(), m01 = matrix.m01(), m02 = matrix.m02(), m03 = matrix.m03();
        float m10 = matrix.m10(), m11 = matrix.m11(), m12 = matrix.m12(), m13 = matrix.m13();
        float m20 = matrix.m20(), m21 = matrix.m21(), m22 = matrix.m22(), m23 = matrix.m23();
        float m30 = matrix.m30(), m31 = matrix.m31(), m32 = matrix.m32(), m33 = matrix.m33();

        planes[0].set(m03 + m00, m13 + m10, m23 + m20, m33 + m30).normalize3(); //left
        planes[1].set(m03 - m00, m13 - m10, m23 - m20, m33 - m30).normalize3(); //right
        planes[2].set(m03 + m01, m13 + m11, m23 + m21, m33 + m31).normalize3(); //bottom
        planes[3].set(m03 - m01, m13 - m11, m23 - m21, m33 - m31).normalize3(); //top
        planes[4].set(m03 + m02, m13 + m12, m23 + m22, m33 + m32).normalize3(); //near
        planes[5].set(m03 - m02, m13 - m12, m23 - m22, m33 - m32).normalize3(); //far
    }

    /**
     * Calculates the signed distance from a plane to a point
     * <p>
     * see {@link #distanceToPoint(Plane, float, float, float)}
     * @param plane The plane for the distance calculation
     * @param x The x-coordinate of the point
     * @param y The y-coordinate of the point
     * @param z The z-coordinate of the point
     * @return A float with the distance of the point from the plane
     */
    private float distanceToPoint(Vector4f plane, float x, float y, float z) {
        return plane.x * x + plane.y * y + plane.z * z + plane.w;
    }

    /**
     * Calculates the signed distance from a plane to a point
     * @param plane The plane for the distance calculation
     * @param x The x-coordinate of the point
     * @param y The y-coordinate of the point
     * @param z The z-coordinate of the point
     * @return A float with the distance of the point from the plane
     * <p>
     * A positive value means the point is on the "inside" side of the plane
     */
    public float distanceToPoint(Plane plane, float x, float y, float z) {
        return distanceToPoint(planes[plane.ordinal()], x, y, z);
    }

    /**
     * Checks if a point is inside or intersecting the frustum
     * @param x The x-coordinate of the point
     * @param y The y-coordinate of the point
     * @param z The z-coordinate of the point
     * @return true if the point is inside the frustum, false otherwise
     */
    public boolean isPointInside(float x, float y, float z) {
        for (int i = 0; i < 6; i++)
            if (distanceToPoint(planes[i], x, y, z) < 0)
                return false; //point is outside
        return true; //point is inside
    }

    /**
     * Checks if a sphere is inside or intersecting the frustum
     * @param x The x-coordinate of the sphere center
     * @param y The y-coordinate of the sphere center
     * @param z The z-coordinate of the sphere center
     * @param radius The radius of the sphere
     * @return true if the sphere is inside or intersecting the frustum, false otherwise
     */
    public boolean isSphereInside(float x, float y, float z, float radius) {
        for (int i = 0; i < 6; i++)
            if (distanceToPoint(planes[i], x, y, z) < -radius)
                return false; //sphere is outside
        return true; //sphere is inside
    }

    /**
     * Checks if an Axis-Aligned Bounding Box (AABB) is intersecting the frustum
     * @param minX Min x-coordinate of the box
     * @param minY Min y-coordinate of the box
     * @param minZ Min z-coordinate of the box
     * @param maxX Max x-coordinate of the box
     * @param maxY Max y-coordinate of the box
     * @param maxZ Max z-coordinate of the box
     * @return true if the box intersects the frustum, false otherwise
     */
    public boolean isBoxInside(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            Vector4f plane = planes[i];

            //pick the "positive vertex" relative to plane normal
            float x = plane.x > 0 ? maxX : minX;
            float y = plane.y > 0 ? maxY : minY;
            float z = plane.z > 0 ? maxZ : minZ;

            if (distanceToPoint(plane, x, y, z) < 0)
                return false; //all points are outside this plane
        }

        return true; //box is inside
    }

    /**
     * Gets the array of frustum planes
     * <p>
     * The order of the planes is:
     * <ul>
     * <li>{@link Plane#LEFT}
     * <li>{@link Plane#RIGHT}
     * <li>{@link Plane#BOTTOM}
     * <li>{@link Plane#TOP}
     * <li>{@link Plane#NEAR}
     * <li>{@link Plane#FAR}
     * </ul>
     * Each plane is represented as a {@link org.joml.Vector4f} in the plane equation {@code x + y + z + w = 0}
     * <p>
     * where {@code x, y, z} is the normal vector of the plane and {@code w} is the distance from the origin
     * @return An array of 6 {@link org.joml.Vector4f} representing the frustum planes
     */
    public Vector4f[] getPlanes() {
        return planes;
    }

    /**
     * Gets the combined projection-view matrix used to extract the frustum planes
     * @return A {@link org.joml.Matrix4f} of the combined projection-view matrix
     */
    public Matrix4f getMatrix() {
        return matrix;
    }

    /**
     * Calculates the 8 corners of a matrix frustum
     * <p>
     * The corners are returned in the following order:
     * <ul>
     * <li>left-bottom-near
     * <li>right-bottom-near
     * <li>left-top-near
     * <li>right-top-near
     * <li>left-bottom-far
     * <li>right-bottom-far
     * <li>left-top-far
     * <li>right-top-far
     * </ul>
     * @return An array of 8 {@link org.joml.Vector4f} representing the frustum corners
     */
    public static Vector4f[] calculateCorners(Matrix4f matrix) {
        Matrix4f inv = new Matrix4f(matrix).invert();
        Vector4f[] corners = new Vector4f[8];
        int i = 0;

        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    corners[i++] = new Vector4f(x, y, z, 1f).mulProject(inv);
                }
            }
        }

        return corners;
    }

    public enum Plane {
        LEFT, RIGHT, BOTTOM, TOP, NEAR, FAR
    }
}
