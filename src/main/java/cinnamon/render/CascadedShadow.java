package cinnamon.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class CascadedShadow {

    public static final float[] CASCADE_SPLITS = new float[]{0.1f, 0.25f, 0.5f, 0.75f};
    private final Matrix4f[] cascadeMatrices = new Matrix4f[]{new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f cullingMatrix = new Matrix4f();
    private final float[] cascadeDistances = new float[CASCADE_SPLITS.length];

    //reusable objects to avoid allocations
    protected final Matrix4f proj = new Matrix4f();
    protected final Matrix4f lightView = new Matrix4f();
    protected final Vector3f frustumCenter = new Vector3f();
    protected final Vector4f trf = new Vector4f();
    protected final float[] frustum = new float[6];

    public void calculateCascadeMatrices(Camera camera, Vector3f lightDir) {
        float nearClip = Camera.NEAR_PLANE;
        float farClip = WorldRenderer.renderDistance;

        int cornerIndex = 0;
        Vector4f[] allCorners = new Vector4f[CASCADE_SPLITS.length * 8];

        for (int i = 0; i < CASCADE_SPLITS.length; i++) {
            float cascadeNear = i == 0 ? nearClip : cascadeDistances[i - 1];
            float cascadeFar = CASCADE_SPLITS[i] * farClip;

            //get frustum corners in world space
            proj.setPerspective((float) Math.toRadians(camera.getFov()), camera.getAspectRatio(), cascadeNear, cascadeFar);
            proj.mul(camera.getViewMatrix());
            Vector4f[] frustumCorners = Frustum.calculateCorners(proj);

            for (Vector4f corner : frustumCorners)
                allCorners[cornerIndex++] = corner;

            //find the center of the frustum
            calculateCenter(frustumCorners);

            //create light view matrix
            createLookMatrix(frustumCenter, lightDir);

            //transform frustum corners to light space and find the min/max extents
            findExtends(frustumCorners, lightView);

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
        calculateCenter(corners);

        //create a single view matrix looking at this center
        createLookMatrix(frustumCenter, lightDir);

        //find the min/max extents of ALL corners in this new light space
        findExtends(corners, lightView);

        //create a single orthographic projection containing everything
        cullingMatrix.setOrtho(frustum[0], frustum[1], frustum[2], frustum[3], frustum[4], frustum[5]).mul(lightView);
    }

    protected void calculateCenter(Vector4f[] corners) {
        frustumCenter.set(0f);
        for (Vector4f corner : corners)
            frustumCenter.add(corner.x, corner.y, corner.z);
        frustumCenter.div(corners.length);
    }

    protected void createLookMatrix(Vector3f center, Vector3f dir) {
        float x = 0f, y = 1f;
        if (Math.abs(dir.y()) > 0.99f) {
            x = 1f; y = 0f;
        }

        lightView.setLookAt(
                center.x - dir.x, center.y - dir.y, center.z - dir.z,
                center.x, center.y, center.z,
                x, y, 0f);
    }

    protected void findExtends(Vector4f[] corners, Matrix4f transform) {
        //minX, maxX, minY, maxY, minZ, maxZ
        frustum[0] = Float.MAX_VALUE; frustum[1] = Float.MIN_VALUE;
        frustum[2] = Float.MAX_VALUE; frustum[3] = Float.MIN_VALUE;
        frustum[4] = Float.MAX_VALUE; frustum[5] = Float.MIN_VALUE;

        for (Vector4f corner : corners) {
            trf.set(corner).mul(transform);
            frustum[0] = Math.min(frustum[0], trf.x); frustum[1] = Math.max(frustum[1], trf.x);
            frustum[2] = Math.min(frustum[2], trf.y); frustum[3] = Math.max(frustum[3], trf.y);
            frustum[4] = Math.min(frustum[4], trf.z); frustum[5] = Math.max(frustum[5], trf.z);
        }
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
