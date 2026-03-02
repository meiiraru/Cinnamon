package cinnamon.world.sky;

import cinnamon.render.Camera;
import cinnamon.render.CubemapRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.SkyBox;

public class DynamicSky extends CubemapSky {

    public static final int CUBEMAP_SIZE = 512;
    protected final CubeMap cubeMap = CubemapRenderer.generateEmptyMap(CUBEMAP_SIZE, CUBEMAP_SIZE, false, false);

    protected final int cubeMap = IBLMap.generateEmptyMap(CUBEMAP_SIZE, false);

    @Override
    protected void renderSky(Camera camera, MatrixStack matrices) {
        this.update();
        super.renderSky(camera, matrices);
    }

    protected void update() {
        Shader prevShader = Shader.activeShader;
        Shader s = Shaders.CUBEMAP_SKYBOX.getShader().use();
        s.setVec3("sunDirection", getRotatedSunDirection());
        s.setColor("skyColor", skyColor);
        s.setColor("sunColor", sunColor);
        s.setColor("fogColor", fogColor);
        s.setFloat("sunIntensity", sunIntensity);
        s.setFloat("fogIntensity", fogIntensity);
        s.setFloat("starsIntensity", starsIntensity);

        CubemapRenderer.renderInvertedCube(cubeMap, s);
        prevShader.use();
    }

    @Override
    protected void bindSkyboxTexture(int index) {
        cubeMap.bind(index);
    }

    @Override
    protected int bindSkybox(Shader shader, int index) {
        cubeMap.bind(index);
        shader.setInt("prefilterMap", index++);

        SkyBox.bindLUT(index);
        shader.setInt("brdfLUT", index);

        return 2;
    }
}
