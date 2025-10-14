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
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class Sky {

    public static final Resource SUN = new Resource("textures/environment/sun.png");

    public int fogColor = 0xBFD3DE;
    public float fogStart = 96;
    public float fogEnd = 192;

    private final Vector3f sunDir = new Vector3f(1, 0, 0);
    private final Matrix3f skyRotation = new Matrix3f();
    private float sunAngle;
    private float sunRoll = Math.toRadians(30f);
    private float cloudSpeed = (float) Math.PI / 2f;

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
        matrices.rotate(Rotation.Z.rotation(sunRoll));
        matrices.rotate(Rotation.X.rotationDeg(-sunAngle));
        matrices.translate(0, 0, 512);

        //render sun
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, -32, -32, 64, 64), SUN);
        VertexConsumer.MAIN.finishBatch(camera);

        //cleanup rendering
        matrices.popMatrix();
    }

    protected void updateSunDir() {
        this.sunDir.set(-1, 0, 0);
        this.sunDir.rotateZ(Math.toRadians(sunAngle));
        this.sunDir.rotateX(sunRoll);

        Rotation.Y.rotationDeg(sunAngle * cloudSpeed).get(this.skyRotation);
    }

    public void setSunAngle(float angle) {
        this.sunAngle = angle;
        updateSunDir();
    }

    public void setSunRoll(float roll) {
        this.sunRoll = Math.toRadians(roll);
        updateSunDir();
    }

    public void setCloudSpeed(float cloudSpeed) {
        this.cloudSpeed = cloudSpeed;
        updateSunDir();
    }

    public Vector3f getSunDirection() {
        return sunDir;
    }

    public Matrix3f getSkyRotation() {
        return skyRotation;
    }

    public int bind(Shader shader, int index) {
        SkyBox box = SkyBox.of(skyBox);
        shader.setMat3("cubemapRotation", getSkyRotation());

        box.bindIrradiance(index);
        shader.setInt("irradianceMap", index++);

        box.bindPrefilter(index);
        shader.setInt("prefilterMap", index++);

        SkyBox.bindLUT(index);
        shader.setInt("brdfLUT", index);

        return 3;
    }

    public void unbind(int index) {
        CubeMap.unbindTex(index++);
        CubeMap.unbindTex(index++);
        Texture.unbindTex(index);
    }

    public void setSkyBox(Resource resource) {
        this.skyBox = resource;
    }
}
