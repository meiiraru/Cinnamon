package cinnamon.world.sky;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.IBLMap;
import cinnamon.render.texture.SkyBox;

import static org.lwjgl.opengl.GL11.glViewport;

public class DynamicSky extends IBLSky {

    public static final int CUBEMAP_SIZE = 512;

    protected final int cubeMap = IBLMap.generateEmptyMap(CUBEMAP_SIZE, false);

    @Override
    protected void renderSky(Camera camera, MatrixStack matrices) {
        this.update();
        super.renderSky(camera, matrices);
    }

    protected void update() {
        Shader old = Shader.activeShader;
        Shader s = Shaders.CUBEMAP_SKYBOX.getShader().use();
        s.setMat4("projection", IBLMap.CAPTURE_PROJECTION);
        s.setVec3("sunDirection", getRotatedSunDirection());
        s.setColor("skyColor", skyColor);
        s.setColor("sunColor", sunColor);
        s.setColor("fogColor", fogColor);
        s.setFloat("sunIntensity", sunIntensity);
        s.setFloat("fogIntensity", fogIntensity);
        s.setFloat("starsIntensity", starsIntensity);

        glViewport(0, 0, CUBEMAP_SIZE, CUBEMAP_SIZE);
        IBLMap.renderInvertedCube(cubeMap, s);

        old.use();
        Framebuffer.activeFramebuffer.adjustViewPort();
    }

    @Override
    protected void bindSkyboxTexture(int index) {
        CubeMap.bind(cubeMap, index);
    }

    @Override
    protected int bindSkybox(Shader shader, int index) {
        CubeMap.bind(cubeMap, index);
        shader.setInt("prefilterMap", index++);

        SkyBox.bindLUT(index);
        shader.setInt("brdfLUT", index);

        return 2;
    }
}
