package cinnamon.world;

import cinnamon.model.GeometryHelper;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.HDRTexture;
import cinnamon.render.texture.IBLMap;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import static cinnamon.Client.LOGGER;

public class SkyBox {

    private static final Resource SUN = new Resource("textures/environment/sun.png");
    private static final float SUN_ROLL = (float) Math.toRadians(30f);
    private static final float CLOUD_SPEED = (float) Math.PI / 2f;

    private final Vector3f sunDir = new Vector3f(1, 0, 0);
    private final Matrix3f skyRotation = new Matrix3f();
    private float sunAngle;

    public Type type = Type.CLOUDS;

    public boolean
            renderSky = true,
            renderSun = true;

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
        Shader.activeShader.setTexture("skybox", type.getTexture(), 0);
        SimpleGeometry.INVERTED_CUBE.render();
        CubeMap.unbindTex(0);
    }

    private void renderSun(Camera camera, MatrixStack matrices) {
        //move to camera position
        matrices.push();
        matrices.translate(camera.getPos());

        //translate sun
        matrices.rotate(Rotation.Y.rotationDeg(90f));
        matrices.rotate(Rotation.Z.rotation(SUN_ROLL));
        matrices.rotate(Rotation.X.rotationDeg(-sunAngle));
        matrices.translate(0, 0, 512);

        //render sun
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, -32, -32, 64, 64), SUN);
        VertexConsumer.MAIN.finishBatch(camera.getPerspectiveMatrix(), camera.getViewMatrix());

        //cleanup rendering
        matrices.pop();
    }

    public void setSunAngle(float angle) {
        this.sunAngle = angle;

        this.sunDir.set(1, 0, 0);
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
        shader.setMat3("cubemapRotation", getSkyRotation());
        shader.setTexture("irradianceMap", type.getIrradiance(), --lastTextureID);
        shader.setTexture("prefilterMap", type.getPrefilter(), --lastTextureID);
        shader.setTexture("brdfLUT", SkyBox.Type.LUT_MAP, --lastTextureID);
    }

    public enum Type {
        CLEAR,
        SPACE,
        CLOUDS,
        TEST,
        HDR_TEST(true),
        WHITE,
        DEBUG;

        public static final Texture LUT_MAP = IBLMap.brdfLUT(512);
        private final boolean hdr;
        private CubeMap texture, irradiance, prefilter;

        Type() {
            this(false);
        }

        Type(boolean hdr) {
            this.hdr = hdr;
        }

        public static void freeAll() {
            for (Type skybox : values())
                skybox.free();
        }

        public static void loadAll() {
            for (Type skybox : values())
                skybox.loadTextures();
        }

        public void loadTextures() {
            Resource res = new Resource("textures/environment/skybox/" + name().toLowerCase() + (hdr ? ".hdr" : ""));
            LOGGER.debug("Loading skybox: {}", res);

            if (hdr) {
                HDRTexture hdrTex = HDRTexture.of(res, true, false);
                texture = IBLMap.hdrToCubemap(hdrTex);
            } else {
                texture = CubeMap.of(res);
            }

            irradiance = IBLMap.generateIrradianceMap(texture);
            prefilter = IBLMap.generatePrefilterMap(texture);
        }

        public void free() {
            texture.free();
            irradiance.free();
            prefilter.free();
        }

        public CubeMap getTexture() {
            return texture;
        }

        public CubeMap getIrradiance() {
            return irradiance;
        }

        public CubeMap getPrefilter() {
            return prefilter;
        }
    }
}
