package mayo.render.framebuffer;

import mayo.render.shader.Shader;
import mayo.utils.Resource;

public enum PostProcess {

    BLIT,
    HDR,
    INVERT,
    BLUR,
    EDGES,
    CHROMATIC_ABERRATION,
    PIXELATE,
    GRAYSCALE;

    public static final PostProcess[] EFFECTS = {INVERT, BLUR, EDGES, CHROMATIC_ABERRATION, PIXELATE, GRAYSCALE};
    private static final Framebuffer POST_FRAMEBUFFER = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);

    private final Resource resource;
    private Shader shader;

    PostProcess() {
        this.resource = new Resource("shaders/post/" + this.name().toLowerCase() + ".glsl");
    }

    private void loadShader() {
        this.shader = new Shader(this.resource);
    }

    public Shader getShader() {
        return shader;
    }

    public void apply() {
        //prepare framebuffer
        POST_FRAMEBUFFER.useClear();

        //fix framebuffer size
        int defaultWidth = Framebuffer.DEFAULT_FRAMEBUFFER.getWidth();
        int defaultHeight = Framebuffer.DEFAULT_FRAMEBUFFER.getHeight();
        if (POST_FRAMEBUFFER.getWidth() != defaultWidth || POST_FRAMEBUFFER.getHeight() != defaultHeight)
            POST_FRAMEBUFFER.resize(defaultWidth, defaultHeight);

        //render post effect
        Blit.prepareShader(Framebuffer.DEFAULT_FRAMEBUFFER, shader);
        Blit.renderQuad();
        Blit.unbindTextures();

        //blit post effect back to main framebuffer
        Blit.copy(POST_FRAMEBUFFER, Framebuffer.DEFAULT_FRAMEBUFFER.id(), BLIT.shader);
    }

    public static void free() {
        for (PostProcess postProcess : values())
            postProcess.shader.free();
    }

    public static void loadAllShaders() {
        for (PostProcess postProcess : values())
            postProcess.loadShader();
    }
}
