package cinnamon.render;

import cinnamon.Client;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.world.sky.Sky;
import cinnamon.world.world.WorldClient;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class RefractionRenderer {

    public static final Framebuffer opaqueLitBuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
    public static final PBRDeferredFramebuffer refractionGBuffer = new PBRDeferredFramebuffer();

    public static void renderRefractions(WorldClient world, float strength, PBRDeferredFramebuffer gBuffer, Framebuffer outputBuffer, boolean renderLights, Sky sky, Camera camera, MatrixStack matrices, float delta) {
        //store gBuffer result in the opaqueLitBuffer
        opaqueLitBuffer.resizeTo(outputBuffer);
        opaqueLitBuffer.use();
        outputBuffer.blit(opaqueLitBuffer, true, true, false);

        //clear only the color buffer of the gBuffer
        gBuffer.blit(refractionGBuffer, false, true, true);
        refractionGBuffer.resizeTo(gBuffer);
        refractionGBuffer.use();
        refractionGBuffer.adjustViewPort();
        refractionGBuffer.clearColors();

        //render the transparent stuff to the gBuffer
        Shader sh = Shaders.GBUFFER_TRANSPARENT.getShader().use();
        sh.setup(camera);
        sh.setVec3("camPos", camera.getPosition());
        sh.setInt("frameIndex", (int) Client.getInstance().frames);
        sh.setTexture("opaqueSceneTex", opaqueLitBuffer.getColorBuffer(), 7);
        sh.setFloat("strength", strength);

        world.renderTransparent(camera, matrices, delta);
        WorldRenderer.finishMaterials(camera);

        //re-render lights
        if (renderLights)
            LightRenderer.renderLights(refractionGBuffer, world.getLights(camera), camera, false, false, () -> {});

        //render the refractions to the output buffer
        outputBuffer.use();
        outputBuffer.adjustViewPort();

        //bake the refraction
        Shader s = Shaders.DEFERRED_TRANSPARENT.getShader().use();
        s.setupInverse(camera);

        int i = 0;
        s.setTexture("gAlbedo",   refractionGBuffer.getAlbedo(),   i++);
        s.setTexture("gNormal",   refractionGBuffer.getNormal(),   i++);
        s.setTexture("gORM",      refractionGBuffer.getORM(),      i++);
        s.setTexture("gEmissive", refractionGBuffer.getEmissive(), i++);
        s.setTexture("lightTex",  LightRenderer.getTexture(), i++);

        s.setFloat("lightFactor", renderLights && LightRenderer.getRenderedLightsCount() > 0 ? 1f : 0f);

        sky.applyUniforms(s, camera);
        sky.bind(s, i);

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        WorldRenderer.renderQuad();
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        Texture.unbindAll(i);
        sky.unbind(i);
    }
}
