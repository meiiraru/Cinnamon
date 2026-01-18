package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.world.Decal;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class DecalRenderer {

    public static void renderDecals(PBRDeferredFramebuffer gBuffer, Camera camera, List<Decal> decals) {
        if (decals.isEmpty())
            return;

        //setup shader
        Shader prev = Shader.activeShader;
        Shader shader = Shaders.DECAL.getShader().use();
        shader.setup(camera);
        shader.setupInverse(camera);
        shader.setTexture("gDepth", gBuffer.getDepthBuffer(), 0);

        //setup buffer
        glDisable(GL_DEPTH_TEST);
        gBuffer.setWriteBuffers(0);

        //render decals
        for (Decal decal : decals) {
            shader.setMat4("model", decal.getModelMatrix());
            shader.setMat4("invModel", decal.getInverseModelMatrix());
            shader.setTexture("textureSampler", Texture.of(decal.getAlbedoTexture()), 1);
            shader.applyColor(decal.getTransform().getColor());
            shader.setFloat("opacity", decal.getOpacity());
            SimpleGeometry.INV_CUBE.render();
        }

        //reset render state
        glEnable(GL_DEPTH_TEST);
        Texture.unbindAll(2);
        gBuffer.resetWriteBuffers();
        prev.use();
    }
}
