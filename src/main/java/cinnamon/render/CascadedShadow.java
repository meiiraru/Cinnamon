package cinnamon.render;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class CascadedShadow {

    public static final int NUM_CASCADES = 4;
    public static float[] CASCADES = {0.01f, 0.05f, 0.13f, 0.3f};
    public static float Z_MULT = 10f;

    private final Matrix4f[] cascadeMatrices;
    private final float[] cascadeDistances;

    //reusable objects to avoid allocations
    protected final Matrix4f
            proj = new Matrix4f(),
            lightView = new Matrix4f(),
            cullBox = new Matrix4f();
    protected final Vector4f cornerLightSpace = new Vector4f();
    protected final Vector3f frustumCenter = new Vector3f();

    public CascadedShadow() {
        this.cascadeMatrices = new Matrix4f[NUM_CASCADES];
        this.cascadeDistances = new float[NUM_CASCADES];

        for (int i = 0; i < NUM_CASCADES; i++)
            cascadeMatrices[i] = new Matrix4f();
    }

    public void calculateCascadeMatrices(Camera camera, Vector3f lightDir) {
        //find the split depths based on view camera frustum
        float near = Camera.NEAR_PLANE;
        float far = WorldRenderer.shadowRenderDistance * 2;

        //calculate each cascade matrix
        for (int i = 0; i < NUM_CASCADES; i++) {
            float nearClip = i == 0 ? near : Math.lerp(near, far, CASCADES[i - 1]);
            float farClip = Math.lerp(near, far, CASCADES[i]);
            float zMult = Z_MULT / (i + 1);
            calculateCascadeMatrices(camera, lightDir, nearClip, farClip, zMult, cascadeMatrices[i]);
            this.cascadeDistances[i] = farClip;
        }

        //calculate the culling box for this shadow map
        calculateCascadeMatrices(camera, lightDir, near, far, Z_MULT, cullBox);
    }

    protected void calculateCascadeMatrices(Camera camera, Vector3f lightDir, float nearClip, float farClip, float zMult, Matrix4f outMatrix) {
        //grab the frustum corners
        proj.setPerspective(Math.toRadians(camera.getFov()), camera.getAspectRatio(), nearClip, farClip);
        proj.mul(camera.getViewMatrix());
        Vector3f[] corners = Frustum.calculateCorners(proj);

        //calculate the center of the frustum slice
        frustumCenter.set(0f);
        for (Vector3f corner : corners)
            frustumCenter.add(corner);
        frustumCenter.div(corners.length);

        //create the light view matrix
        float x = 0f, y = 1f;
        if (Math.abs(lightDir.y()) > 0.999f) {
            x = 1f; y = 0f;
        }

        lightView.setLookAt(
                frustumCenter.x - lightDir.x, frustumCenter.y - lightDir.y, frustumCenter.z - lightDir.z,
                frustumCenter.x, frustumCenter.y, frustumCenter.z,
                x, y, 0f
        );

        //find the min/max coordinates of the frustum corners in light space
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

        for (Vector3f corner : corners) {
            cornerLightSpace.set(corner, 1f).mul(lightView);
            minX = Math.min(minX, cornerLightSpace.x); maxX = Math.max(maxX, cornerLightSpace.x);
            minY = Math.min(minY, cornerLightSpace.y); maxY = Math.max(maxY, cornerLightSpace.y);
            minZ = Math.min(minZ, cornerLightSpace.z); maxZ = Math.max(maxZ, cornerLightSpace.z);
        }

        //extend z range to ensure objects slightly outside the frustum are included
        minZ = minZ < 0 ? minZ * zMult : minZ / zMult;
        maxZ = maxZ < 0 ? maxZ / zMult : maxZ * zMult;

        //create the light orthographic projection matrix
        outMatrix.setOrtho(minX, maxX, minY, maxY, minZ, maxZ).mul(lightView);
    }

    public Matrix4f[] getCascadeMatrices() {
        return this.cascadeMatrices;
    }

    public float[] getCascadeDistances() {
        return this.cascadeDistances;
    }

    public Matrix4f getCullingMatrix() {
        return cullBox;
    }
}
