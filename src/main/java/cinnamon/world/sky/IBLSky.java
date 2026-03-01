package cinnamon.world.sky;

import cinnamon.model.SimpleGeometry;
import cinnamon.registry.SkyBoxRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.SkyBox;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class IBLSky extends Sky {

    protected final Matrix3f skyRotation = new Matrix3f();
    protected Resource skyBox = SkyBoxRegistry.CLOUDS.resource;
    protected float rotationSpeed = Math.PI_OVER_2_f;

    @Override
    protected void renderSky(Camera camera, MatrixStack matrices) {
        //render model
        Shader o = Shader.activeShader;
        Shader s = Shaders.SKYBOX.getShader().use();
        s.setup(camera);
        s.setMat3("rotation", skyRotation);
        s.setInt("skybox", 0);
        bindSkyboxTexture(0);
        SimpleGeometry.INV_CUBE.render();
        CubeMap.unbindTex(0);
        o.use();
    }

    protected void bindSkyboxTexture(int index) {
        SkyBox.of(skyBox).bind(index);
    }

    @Override
    protected void updateSunDir() {
        super.updateSunDir();
        Rotation.Y.rotationDeg(sunAngle * rotationSpeed).get(skyRotation);
    }

    public Matrix3f getSkyRotation() {
        return skyRotation;
    }

    @Override
    public int bind(Shader shader, int index) {
        shader.setMat3("cubemapRotation", getSkyRotation());
        return bindSkybox(shader, index);
    }

    protected int bindSkybox(Shader shader, int index) {
        SkyBox box = SkyBox.of(skyBox);

        //box.bindIrradiance(index);
        //shader.setInt("irradianceMap", index++);

        box.bindPrefilter(index);
        shader.setInt("prefilterMap", index++);

        SkyBox.bindLUT(index);
        shader.setInt("brdfLUT", index);

        return 2;
    }

    @Override
    public void unbind(int index) {
        //CubeMap.unbindTex(index++); //irradiance
        CubeMap.unbindTex(index++); //prefilter
        Texture.unbindTex(index); //brdf LUT
    }

    public void setSkyBox(Resource resource) {
        this.skyBox = resource;
    }

    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
        this.updateSunDir();
    }

    public Vector3f getRotatedSunDirection() {
        return getSkyRotation().transformTranspose(getSunDirection(), new Vector3f());
    }
}
