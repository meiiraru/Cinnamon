package mayo.world;

import mayo.model.GeometryHelper;
import mayo.model.SimpleGeometry;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.render.texture.CubeMap;
import mayo.render.texture.HDRTexture;
import mayo.render.texture.IBLMap;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class SkyBox {

    private static final Resource SUN = new Resource("textures/environment/sun.png");
    private static final float SUN_ROLL = (float) Math.toRadians(30f);
    private static final float CLOUD_SPEED = (float) Math.PI / 2f;

    private final Vector3f sunDir = new Vector3f(1, 0, 0);
    private float sunAngle;
    private Matrix3f skyRotation;

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
        type.bindTexture();
        glDepthMask(false);
        SimpleGeometry.INVERTED_CUBE.render();
        glDepthMask(true);
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

        this.skyRotation = Rotation.Y.rotationDeg(sunAngle * CLOUD_SPEED).get(new Matrix3f());
    }

    public Vector3f getSunDirection() {
        return sunDir;
    }

    public Matrix3f getSkyRotation() {
        return skyRotation;
    }

    public enum Type {
        CLEAR,
        SPACE,
        CLOUDS,
        TEST,
        HDR_TEST(true);

        private static final int LUT_MAP = IBLMap.brdfLUT(512);
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
            if (hdr) {
                HDRTexture hdrTex = HDRTexture.of(new Resource("textures/environment/skybox/" + name().toLowerCase() + ".hdr"));
                texture = IBLMap.hdrToCubemap(hdrTex);
            } else {
                texture = CubeMap.of(new Resource("textures/environment/skybox/" + name().toLowerCase()));
            }

            irradiance = IBLMap.generateIrradianceMap(texture);
            prefilter = IBLMap.generatePrefilterMap(texture);
        }

        public void free() {
            texture.free();
            irradiance.free();
            prefilter.free();
        }

        public void bindTexture() {
            texture.bind();
        }

        public void bindIrradiance() {
            irradiance.bind();
        }

        public void bindPrefilter() {
            prefilter.bind();
        }

        public void bindLUT() {
            glBindTexture(GL_TEXTURE_2D, LUT_MAP);
        }
    }
}
