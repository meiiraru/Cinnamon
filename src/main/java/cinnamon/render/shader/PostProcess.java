package cinnamon.render.shader;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.WorldRenderer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;

import java.util.function.BiFunction;

import static cinnamon.render.shader.PostProcess.FB.COLOR_UNIFORM;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public enum PostProcess {

    //specials
    BLIT(COLOR_UNIFORM),
    BLIT_GAMMA(COLOR_UNIFORM),
    KERNEL((fb, s) -> {
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        return COLOR_UNIFORM.apply(fb, s);
    }),
    LOOKUP_TEXTURE(COLOR_UNIFORM),
    GAUSSIAN_BLUR((fb, s) -> {
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setVec2("dir", 1f, 1f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    TONEMAPPING(COLOR_UNIFORM),

    //effects
    INVERT(COLOR_UNIFORM),
    BLUR((fb, s) -> {
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setVec2("dir", 1f, 1f);
        s.setFloat("radius", 5f);
        return COLOR_UNIFORM.apply(fb, s);
    }),
    EDGES((fb, s) -> {
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        return COLOR_UNIFORM.apply(fb, s);
    }),
    CHROMATIC_ABERRATION((fb, s) -> {
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
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
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
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
        s.applyColorRGBA(0xFFFFFFFF);
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
    RED(LOOKUP_TEXTURE.resource, (fb, s) -> {
        int i = LOOKUP_TEXTURE.uniformFunction.apply(fb, s);
        s.setTexture("lutTex", Texture.of(new Resource("textures/shader/lut/red.png")), i++);
        s.setVec2("lutGrid", 8f, 8f);
        return i;
    }),
    TILT_SHIFT((fb, s) -> {
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        return COLOR_UNIFORM.apply(fb, s);
    }),

    //world only effects
    TOON_OUTLINE((fb, s) -> {
        int i = COLOR_UNIFORM.apply(fb, s);
        s.setTexture("depthTex", WorldRenderer.PBRFrameBuffer.getDepthBuffer(), i++);
        s.setTexture("normalTex", WorldRenderer.PBRFrameBuffer.gNormal, i++);
        s.setVec2("texelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setVec2("depthBias", 64f, 1f);
        s.setVec2("normalBias", 1f, 16f);
        s.setVec3("outlineColor", 0f, 0f, 0f);
        return i;
    });

    public static final PostProcess[] EFFECTS = {
            INVERT, BLUR, EDGES, CHROMATIC_ABERRATION, PIXELATE, GRAYSCALE,
            SCAN_LINE, LENS, LENS2, MICROWAVE_SCREEN, UPSIDE_DOWN, TRIPPY,
            KALEIDOSCOPE, BITS, POSTERIZE, BLOBS, PHOSPHOR, SPEED_LINES, DOT_GRID,
            DITHER, DITHER_SQUARE_TEX, SHARPEN, VINTAGE, RED, TILT_SHIFT
    };
    public static final PostProcess[] WORLD_EFFECTS = {
            TOON_OUTLINE
    };

    public static boolean saveLastColor = false;

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

        //disable alpha blending and depth test
        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);

        //prepare framebuffer
        Framebuffer originalFb = Framebuffer.activeFramebuffer;
        FB.PING.resizeTo(originalFb);
        FB.PONG.resizeTo(originalFb);

        Framebuffer source = originalFb;
        Framebuffer destination = FB.PING;

        for (PostProcess postProcess : postProcesses) {
            if (postProcess == null)
                continue;

            //render post effect
            destination.useClear();
            int tex = postProcess.uniformFunction.apply(source, postProcess.shader.use());
            SimpleGeometry.QUAD.render();
            Texture.unbindAll(tex);

            PostProcess.saveLastColor |= postProcess.usesPrevColor;

            //ping pong
            source = destination;
            destination = source == FB.PING ? FB.PONG : FB.PING;
        }

        source.blit(originalFb.id());
        originalFb.use();

        //restore state
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    public static void finishFrame() {
        if (!saveLastColor)
            return;

        //copy from main buffer to previous color buffer
        Framebuffer main = Framebuffer.DEFAULT_FRAMEBUFFER;
        FB.PREVIOUS_COLOR_FRAMEBUFFER.resizeTo(main);
        FB.PREVIOUS_COLOR_FRAMEBUFFER.useClear();

        main.blit(FB.PREVIOUS_COLOR_FRAMEBUFFER.id());
        main.use();
        saveLastColor = false;
    }

    //wacky hack
    public static final class FB {
        public static final Framebuffer
                PING = new Framebuffer(Framebuffer.COLOR_BUFFER),
                PONG = new Framebuffer(Framebuffer.COLOR_BUFFER),
                PREVIOUS_COLOR_FRAMEBUFFER = new Framebuffer(Framebuffer.COLOR_BUFFER);

        public static final BiFunction<Framebuffer, Shader, Integer>
                DEPTH_UNIFORM = (fb, s) -> {
                    s.setTexture("depthTex", fb.getDepthBuffer(), 0);
                    return 1;
                },
                COLOR_UNIFORM = (fb, s) -> {
                    s.setTexture("colorTex", fb.getColorBuffer(), 0);
                    return 1;
                },
                COLOR_AND_DEPTH_UNIFORM = (fb, s) -> {
                    s.setTexture("colorTex", fb.getColorBuffer(), 0);
                    s.setTexture("depthTex", fb.getDepthBuffer(), 1);
                    return 2;
                };
    }
}
