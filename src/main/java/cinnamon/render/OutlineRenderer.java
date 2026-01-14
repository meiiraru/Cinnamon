package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;

import java.util.function.Consumer;

import static cinnamon.render.WorldRenderer.renderQuad;

public class OutlineRenderer {

    public static final Framebuffer outlineFramebuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);
    private static Framebuffer target;

    public static void prepareRenderer(Framebuffer outputBuffer, Camera camera) {
        target = outputBuffer;

        //prepare outline framebuffer and shaders
        outlineFramebuffer.resizeTo(target);
        outlineFramebuffer.useClear();

        Shader main = Shaders.MAIN_PASS.getShader();
        Shader model = Shaders.MODEL_PASS.getShader();
        main.use().setup(camera);
        model.use().setup(camera);
    }

    public static void bakeOutlines(Consumer<Shader> shaderConsumer) {
        //apply outlines to the buffer
        target.use();

        //prepare shader
        Shader s = Shaders.OUTLINE.getShader().use();
        s.setTexture("outlineTex", outlineFramebuffer.getColorBuffer(), 0);
        s.setVec2("texelSize", 1f / outlineFramebuffer.getWidth(), 1f / outlineFramebuffer.getHeight());
        s.setFloat("radius", 4f);

        if (shaderConsumer != null)
            shaderConsumer.accept(s);

        //render outline
        renderQuad();

        //cleanup
        Texture.unbindTex(0);
    }
}
