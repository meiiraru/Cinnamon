package cinnamon.world;

import cinnamon.model.GeometryHelper;
import cinnamon.model.SimpleGeometry;
import cinnamon.registry.SkyBoxRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.SkyBox;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class Sky {

    public static final Resource SUN = new Resource("textures/environment/sun.png");
    public static final float SUN_ROLL = (float) Math.toRadians(30f);
    public static final float CLOUD_SPEED = (float) Math.PI / 2f;

    public int fogColor = 0xBFD3DE;
    public float fogStart = 96;
    public float fogEnd = 192;

    private final Vector3f sunDir = new Vector3f(1, 0, 0);
    private final Matrix3f skyRotation = new Matrix3f();
    private float sunAngle;

    private Resource skyBox = SkyBoxRegistry.CLOUDS.resource;

    public boolean
            renderSky = true,
            renderSun = false;

    public void render(Camera camera, MatrixStack matrices) {
        //render sky
        if (renderSky)
            renderSky();

        //render sun
        if (renderSun)
            renderSun(camera, matrices);
    }

    private void renderSky() {
        //render model
        Shader.activeShader.setMat3("rotation", skyRotation);
        Shader.activeShader.setInt("skybox", 0);
        SkyBox.of(skyBox).bind(0);
        SimpleGeometry.INVERTED_CUBE.render();
        CubeMap.unbindTex(0);
    }

    private void renderSun(Camera camera, MatrixStack matrices) {
        //move to camera position
        matrices.pushMatrix();
        matrices.translate(camera.getPos());

        //translate sun
        matrices.rotate(Rotation.Y.rotationDeg(90f));
        matrices.rotate(Rotation.Z.rotation(SUN_ROLL));
        matrices.rotate(Rotation.X.rotationDeg(-sunAngle));
        matrices.translate(0, 0, 512);

        //render sun
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, -32, -32, 64, 64), SUN);
        VertexConsumer.MAIN.finishBatch(camera);

        //cleanup rendering
        matrices.popMatrix();
    }

    public void setSunAngle(float angle) {
        this.sunAngle = angle;

        this.sunDir.set(-1, 0, 0);
        this.sunDir.rotateZ((float) Math.toRadians(sunAngle));
        this.sunDir.rotateX(SUN_ROLL);

        Rotation.Y.rotationDeg(sunAngle * CLOUD_SPEED).get(this.skyRotation);
    }

    public Vector3f getSunDirection() {
        return sunDir;
    }

    public Matrix3f getSkyRotation() {
        return skyRotation;
    }

    public void pushToShader(Shader shader, int lastTextureID) {
        SkyBox box = SkyBox.of(skyBox);
        shader.setMat3("cubemapRotation", getSkyRotation());
        int i = lastTextureID - 3;
        shader.setInt("irradianceMap", i);
        shader.setInt("prefilterMap", i + 1);
        shader.setInt("brdfLUT", i + 2);
        box.bindIBL(i);
    }

    public void setSkyBox(Resource resource) {
        this.skyBox = resource;
    }
}
