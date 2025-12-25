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
import org.joml.Matrix3f;

public class IBLSky extends Sky {

    private final Matrix3f skyRotation = new Matrix3f();
    private Resource skyBox = SkyBoxRegistry.CLOUDS.resource;

    @Override
    protected void renderSky(Camera camera, MatrixStack matrices) {
        //render model
        Shader s = Shaders.SKYBOX.getShader().use();
        s.setup(camera);
        s.setMat3("rotation", skyRotation);
        s.setInt("skybox", 0);
        SkyBox.of(skyBox).bind(0);
        SimpleGeometry.INVERTED_CUBE.render();
        CubeMap.unbindTex(0);
    }

    @Override
    protected void updateSunDir() {
        super.updateSunDir();
        Rotation.Y.rotationDeg(sunAngle * cloudSpeed).get(this.skyRotation);
    }

    public Matrix3f getSkyRotation() {
        return skyRotation;
    }

    @Override
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

    @Override
    public void unbind(int index) {
        CubeMap.unbindTex(index++);
        CubeMap.unbindTex(index++);
        Texture.unbindTex(index);
    }

    public void setSkyBox(Resource resource) {
        this.skyBox = resource;
    }
}
