package mayo.render.shader;

import mayo.render.framebuffer.Blit;
import mayo.render.framebuffer.Framebuffer;
import mayo.render.texture.Texture;
import mayo.utils.Resource;

import java.util.function.BiFunction;

import static mayo.render.framebuffer.Blit.SCREEN_TEX_ONLY_UNIFORM;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public enum PostProcess {

    BLIT(SCREEN_TEX_ONLY_UNIFORM),
    HDR(SCREEN_TEX_ONLY_UNIFORM),
    INVERT(SCREEN_TEX_ONLY_UNIFORM),
    BLUR((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setVec2("dir", 1f, 1f);
        s.setFloat("radius", 5f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    EDGES((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    CHROMATIC_ABERRATION((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setFloat("intensity", 2.5f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    PIXELATE((fb, s) -> {
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("factor", 8f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    GRAYSCALE(SCREEN_TEX_ONLY_UNIFORM),
    SCAN_LINE((fb, s) -> {
        s.setFloat("time", (float) glfwGetTime() * 0.01f);
        s.setFloat("density", 0.9f * fb.getHeight());
        s.setFloat("opacity", 0.3f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    LENS((fb, s) -> {
        s.setVec2("distortion", -0.5f, -0.5f);
        s.setFloat("focus", 1f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    LENS2(LENS.resource, (fb, s) -> {
        s.setVec2("distortion", 1f, 1f);
        s.setFloat("focus", 0.4f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    MICROWAVE_SCREEN((fb, s) -> {
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("cellSize", 10f);
        s.setFloat("fill", 0.8f);
        s.setFloat("opacity", 0.4f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    UPSIDE_DOWN(SCREEN_TEX_ONLY_UNIFORM),
    TRIPPY((fb, s) -> {
        s.setVec2("resolution", fb.getWidth(), fb.getHeight());
        s.setFloat("count", 1f);
        s.setFloat("time", (float) glfwGetTime() * 0.25f);
        s.setFloat("waveSpeed", 10f);
        s.setFloat("waveStrength", 0.005f);
        s.setFloat("waveFrequency", 24f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    KALEIDOSCOPE((fb, s) -> {
        s.setFloat("segments", 5f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    BITS((fb, s) -> {
        s.setInt("bits", 2);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    }),
    BLOBS((fb, s) -> {
        s.setVec2("textelSize", 1f / fb.getWidth(), 1f / fb.getHeight());
        s.setFloat("radius", 7f);
        return SCREEN_TEX_ONLY_UNIFORM.apply(fb, s);
    });

    public static final PostProcess[] EFFECTS = {
            INVERT, BLUR, EDGES, CHROMATIC_ABERRATION, PIXELATE, GRAYSCALE, SCAN_LINE,
            LENS, LENS2, MICROWAVE_SCREEN, UPSIDE_DOWN, TRIPPY, KALEIDOSCOPE, BITS, BLOBS
    };
    private static final Framebuffer POST_FRAMEBUFFER = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);

    private final Resource resource;
    private final BiFunction<Framebuffer, Shader, Integer> uniformFunction;
    private Shader shader;

    PostProcess(BiFunction<Framebuffer, Shader, Integer> uniformFunction) {
        this.resource = new Resource("shaders/post/" + this.name().toLowerCase() + ".glsl");
        this.uniformFunction = uniformFunction;
    }

    PostProcess(Resource shaderSrc, BiFunction<Framebuffer, Shader, Integer> uniformFunction) {
        this.resource = shaderSrc;
        this.uniformFunction = uniformFunction;
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

    private void render(Framebuffer framebuffer) {
        int tex = Blit.prepareShader(framebuffer, shader, uniformFunction);
        Blit.renderQuad();
        Texture.unbindAll(tex);
    }

    public void apply(PostProcess... postProcesses) {
        //prepare framebuffer
        POST_FRAMEBUFFER.useClear();
        POST_FRAMEBUFFER.resizeTo(Framebuffer.DEFAULT_FRAMEBUFFER);

        //render post effect
        render(Framebuffer.DEFAULT_FRAMEBUFFER);

        //render additional post effects
        for (PostProcess postProcess : postProcesses) {
            glClear(GL_DEPTH_BUFFER_BIT);
            postProcess.render(POST_FRAMEBUFFER);
        }

        //blit everything back to main framebuffer
        Blit.copy(POST_FRAMEBUFFER, Framebuffer.DEFAULT_FRAMEBUFFER.id(), BLIT);
    }
}
