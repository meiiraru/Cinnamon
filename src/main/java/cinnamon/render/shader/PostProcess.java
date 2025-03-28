package cinnamon.render.shader;

import cinnamon.render.WorldRenderer;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;

import java.util.function.BiFunction;

import static cinnamon.render.framebuffer.Blit.COLOR_UNIFORM;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

public enum PostProcess {

    //specials
    BLIT(COLOR_UNIFORM),
    BLIT_GAMMA(COLOR_UNIFORM),
    HDR(COLOR_UNIFORM),
    KERNEL((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        return COLOR_UNIFORM.apply(fb, s);
    }),
    LOOKUP_TEXTURE(COLOR_UNIFORM),

    //effects
    INVERT(COLOR_UNIFORM),
    BLUR((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setVec2("dir", 1f, 1f);
        s.setFloat("radius", 5f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    EDGES((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        return COLOR_UNIFORM.apply(fb, s);
    }),
    CHROMATIC_ABERRATION((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setFloat("intensity", 2.5f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    PIXELATE((fb, s) -> {
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("factor", 8f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    GRAYSCALE(COLOR_UNIFORM),
    SCAN_LINE((fb, s) -> {
        s.setFloat("time", (float) glfwGetTime() * 0.01f);
        s.setFloat("density", 0.9f * fb.getHeight());
        s.setFloat("opacity", 0.3f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    LENS((fb, s) -> {
        s.setVec2("distortion", -0.5f, -0.5f);
        s.setFloat("focus", 1f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    LENS2(LENS.resource, (fb, s) -> {
        s.setVec2("distortion", 1f, 1f);
        s.setFloat("focus", 0.4f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    MICROWAVE_SCREEN((fb, s) -> {
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("cellSize", 10f);
        s.setFloat("fill", 0.8f);
        s.setFloat("opacity", 0.2f);
        s.setVec2("borders", 1f, 2f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    UPSIDE_DOWN(COLOR_UNIFORM),
    TRIPPY((fb, s) -> {
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("count", 1f);
        s.setFloat("time", (float) glfwGetTime() * 0.25f);
        s.setFloat("waveSpeed", 10f);
        s.setFloat("waveStrength", 0.005f);
        s.setFloat("waveFrequency", 24f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    KALEIDOSCOPE((fb, s) -> {
        s.setFloat("segments", 5f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    BITS((fb, s) -> {
        s.setInt("bits", 2);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    BLOBS((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setFloat("radius", 7f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    PHOSPHOR((fb, s) -> {
        s.setFloat("phosphor", 0.97f);
        int i = COLOR_UNIFORM.apply(fb, s);
        s.setTexture("prevColorTex", FB.PREVIOUS_COLOR_FRAMEBUFFER.getColorBuffer(), i++);
        return i;
    }, true),
    SPEED_LINES((fb, s) -> {
        s.setFloat("time", (float) glfwGetTime());
        s.setFloat("speed", 5f);
        s.setFloat("lines", 200f);
        s.setFloat("rotationSpeed", 3f);
        s.setFloat("intensity", 0.2f);
        s.setFloat("maskSize", 0.5f);
        s.setFloat("maskStrength", 0.5f);
        s.applyColor(0xFFFFFF);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    DOT_GRID((fb, s) -> {
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("cellSize", 12f);
        s.setFloat("fill", 0.4f);
        s.setFloat("opacity", 0.85f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    POSTERIZE((fb, s) -> {
        s.setInt("colorCount", 8);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    DITHER((fb, s) -> {
        int i = COLOR_UNIFORM.apply(fb, s);
        s.setTexture("ditherTex", Texture.of(new Resource("textures/shader/dither/dither.png")), i++);
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        return i;
    }),
    DITHER_SQUARE_TEX((fb, s) -> {
        int i = COLOR_UNIFORM.apply(fb, s);
        s.setTexture("ditherTex", Texture.of(new Resource("textures/shader/dither/lines.png")), i++);
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("colorMask", 0.2f);
        return i;
    }),
    SHARPEN(KERNEL.resource, (fb, s) -> {
        s.setMat3("kernel", 0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f);
        return KERNEL.uniformFunction.apply(fb, s);
    }),
    VINTAGE(LOOKUP_TEXTURE.resource, (fb, s) -> {
        int i = LOOKUP_TEXTURE.uniformFunction.apply(fb, s);
        s.setTexture("lutTex", Texture.of(new Resource("textures/shader/lut/vintage.png")), i++);
        s.setVec2("lutGrid", 8f, 8f);
        return i;
    }),

    //world only effects
    TOON_OUTLINE((fb, s) -> {
        int i = COLOR_UNIFORM.apply(fb, s);
        s.setTexture("depthTex", WorldRenderer.PBRFrameBuffer.getDepthBuffer(), i++);
        s.setTexture("normalTex", WorldRenderer.PBRFrameBuffer.gNormal, i++);
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setVec2("depthBias", 64f, 1f);
        s.setVec2("normalBias", 1f, 16f);
        s.setVec3("outlineColor", 0f, 0f, 0f);
        return i;
    });

    public static final PostProcess[] EFFECTS = {
            INVERT, BLUR, EDGES, CHROMATIC_ABERRATION, PIXELATE, GRAYSCALE,
            SCAN_LINE, LENS, LENS2, MICROWAVE_SCREEN, UPSIDE_DOWN, TRIPPY,
            KALEIDOSCOPE, BITS, POSTERIZE, BLOBS, PHOSPHOR, SPEED_LINES, DOT_GRID,
            DITHER, DITHER_SQUARE_TEX, SHARPEN, VINTAGE
    };
    public static final PostProcess[] WORLD_EFFECTS = {
            TOON_OUTLINE
    };

    private final Resource resource;
    private final BiFunction<Framebuffer, Shader, Integer> uniformFunction;
    private final boolean usesPrevColor;
    private Shader shader;

    PostProcess(BiFunction<Framebuffer, Shader, Integer> uniformFunction) {
        this(uniformFunction, false);
    }

    PostProcess(BiFunction<Framebuffer, Shader, Integer> uniformFunction, boolean usesPrevColor) {
        this(null, uniformFunction, usesPrevColor);
    }

    PostProcess(Resource shaderSrc, BiFunction<Framebuffer, Shader, Integer> uniformFunction) {
        this(shaderSrc, uniformFunction, false);
    }

    PostProcess(Resource shaderSrc, BiFunction<Framebuffer, Shader, Integer> uniformFunction, boolean usesPrevColor) {
        this.resource = shaderSrc == null ? new Resource("shaders/post/" + this.name().toLowerCase() + ".glsl") : shaderSrc;
        this.uniformFunction = uniformFunction;
        this.usesPrevColor = usesPrevColor;
    }

    public static void free() {
        for (PostProcess postProcess : values())
            postProcess.shader.free();
    }

    public static void loadAllShaders() {
        for (PostProcess postProcess : values())
            postProcess.loadShader();
    }

    private void loadShader() {
        this.shader = new Shader(this.resource);
    }

    public Shader getShader() {
        return shader;
    }

    public BiFunction<Framebuffer, Shader, Integer> uniformFunction() {
        return uniformFunction;
    }

    public static void apply(PostProcess... postProcesses) {
        if (postProcesses.length == 0)
            return;

        //prepare framebuffer
        Framebuffer old = Framebuffer.activeFramebuffer;
        FB.POST_FRAMEBUFFER.useClear();
        FB.POST_FRAMEBUFFER.resizeTo(old);

        boolean savePrevColor = false;

        for (PostProcess postProcess : postProcesses) {
            if (postProcess == null)
                continue;

            //render post effect
            FB.POST_FRAMEBUFFER.use();
            int tex = postProcess.uniformFunction.apply(old, postProcess.shader.use());
            Blit.renderQuad();
            Texture.unbindAll(tex);

            savePrevColor |= postProcess.usesPrevColor;

            //blit to main framebuffer
            Blit.copy(FB.POST_FRAMEBUFFER, old.id(), BLIT);
        }

        //if any effect uses the previous color buffer, copy buffers again
        if (savePrevColor) {
            FB.PREVIOUS_COLOR_FRAMEBUFFER.resizeTo(old);
            Blit.copy(old, FB.PREVIOUS_COLOR_FRAMEBUFFER.id(), BLIT);
            old.use();
        }
    }

    //wacky hack
    private static final class FB {
        private static final Framebuffer
                POST_FRAMEBUFFER = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER),
                PREVIOUS_COLOR_FRAMEBUFFER = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER);
    }
}
