package cinnamon.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class CascadedShadow {

    public static final float[] CASCADE_SPLITS = new float[]{0.1f, 0.25f, 0.5f, 0.75f};
    private final Matrix4f[] cascadeMatrices = new Matrix4f[]{new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f cullingMatrix = new Matrix4f();
    private final float[] cascadeDistances = new float[CASCADE_SPLITS.length];

    public void calculateCascadeMatrices(Camera camera, Vector3f lightDir) {
        float nearClip = Camera.NEAR_PLANE;
        float farClip = WorldRenderer.renderDistance;

        int cornerIndex = 0;
        Vector4f[] allCorners = new Vector4f[CASCADE_SPLITS.length * 8];

        for (int i = 0; i < CASCADE_SPLITS.length; i++) {
            float cascadeNear = i == 0 ? nearClip : cascadeDistances[i - 1];
            float cascadeFar = CASCADE_SPLITS[i] * farClip;

            //get frustum corners in world space
            Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(camera.getFov()), camera.getAspectRatio(), cascadeNear, cascadeFar);
            proj.mul(camera.getViewMatrix());
            Vector4f[] frustumCorners = Frustum.calculateCorners(proj);

            for (Vector4f corner : frustumCorners)
                allCorners[cornerIndex++] = corner;

            //find the center of the frustum
            Vector3f frustumCenter = calculateCenter(frustumCorners);

            //create light view matrix
            Matrix4f lightView = createLookMatrix(frustumCenter, lightDir);

            //transform frustum corners to light space and find the min/max extents
            float[] frustum = findExtends(frustumCorners, lightView);

            //extend z range to ensure objects slightly outside the frustum are included
            float zMult = 10f;
            frustum[4] = frustum[4] < 0 ? frustum[4] * zMult : frustum[4] / zMult;
            frustum[5] = frustum[5] < 0 ? frustum[5] / zMult : frustum[5] * zMult;

            //store the orthographic projection matrix
            cascadeMatrices[i].setOrtho(frustum[0], frustum[1], frustum[2], frustum[3], frustum[4], frustum[5]).mul(lightView);
            cascadeDistances[i] = cascadeFar;
        }

        //calculate a single culling matrix containing all cascades
        calculateCullingMatrix(allCorners, lightDir);
    }

    protected void calculateCullingMatrix(Vector4f[] corners, Vector3f lightDir) {
        //find the center of the entire bounding volume
        Vector3f totalCenter = calculateCenter(corners);

        //create a single view matrix looking at this center
        Matrix4f lightView = createLookMatrix(totalCenter, lightDir);

        //find the min/max extents of ALL corners in this new light space
        float[] frustum = findExtends(corners, lightView);

        //create a single orthographic projection containing everything
        cullingMatrix.setOrtho(frustum[0], frustum[1], frustum[2], frustum[3], frustum[4], frustum[5]).mul(lightView);
    }

    protected static Vector3f calculateCenter(Vector4f[] corners) {
        Vector3f center = new Vector3f();
        for (Vector4f corner : corners)
            center.add(corner.x, corner.y, corner.z);
        return center.div(corners.length);
    }

    protected static Matrix4f createLookMatrix(Vector3f center, Vector3f dir) {
        float x = 0f, y = 1f;
        if (Math.abs(dir.y()) > 0.99f) {
            x = 1f; y = 0f;
        }

        return new Matrix4f().lookAt(
                center.x - dir.x, center.y - dir.y, center.z - dir.z,
                center.x, center.y, center.z,
                x, y, 0f);
    }

    protected static float[] findExtends(Vector4f[] corners, Matrix4f transform) {
        float[] extents = { //minX, maxX, minY, maxY, minZ, maxZ
                Float.MAX_VALUE, Float.MIN_VALUE,
                Float.MAX_VALUE, Float.MIN_VALUE,
                Float.MAX_VALUE, Float.MIN_VALUE
        };

        for (Vector4f corner : corners) {
            Vector4f trf = new Vector4f(corner).mul(transform); //create a copy to multiply
            extents[0] = Math.min(extents[0], trf.x); extents[1] = Math.max(extents[1], trf.x);
            extents[2] = Math.min(extents[2], trf.y); extents[3] = Math.max(extents[3], trf.y);
            extents[4] = Math.min(extents[4], trf.z); extents[5] = Math.max(extents[5], trf.z);
        }

        return extents;
    }

    public Matrix4f[] getCascadeMatrices() {
        return cascadeMatrices;
    }

    public float[] getCascadeDistances() {
        return cascadeDistances;
    }

    public Matrix4f getCullingMatrix() {
        return cullingMatrix;
    }

    public static int getNumCascades() {
        return CASCADE_SPLITS.length;
    }
}
