package mayo.render.framebuffer;

import mayo.Client;
import mayo.render.Window;
import mayo.render.shader.Shader;
import mayo.utils.Resource;

import static org.lwjgl.opengl.GL15.*;

public enum PostProcess {

    BLIT,
    INVERT,
    BLUR,
    EDGES,
    CHROMATIC_ABERRATION,
    PIXELATE;

    private final Resource resource;
    private Shader shader;

    PostProcess() {
        this.resource = new Resource("shaders/post/" + this.name().toLowerCase() + ".glsl");
    }

    private void loadShader() {
        this.shader = new Shader(this.resource);
    }

    public static void loadAllShaders() {
        for (PostProcess postProcess : values())
            postProcess.loadShader();
    }

    // -- static framebuffer stuff -- //

    public static final Framebuffer frameBuffer;

    static {
        Window w = Client.getInstance().window;
        frameBuffer = new Framebuffer(w.width, w.height, Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
    }

    public static void prepare() {
        frameBuffer.use();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        frameBuffer.clear();
    }

    public static void render(PostProcess effect) {
        render(effect, 0);
    }

    public static void render(PostProcess effect, int targetFramebufferID) {
        Blit.copy(frameBuffer, targetFramebufferID, effect.shader);
    }

    public static void free() {
        for (PostProcess value : PostProcess.values())
            value.shader.free();
        frameBuffer.free();
    }

    public static void resize(int width, int height) {
        frameBuffer.resize(width, height);
    }
}
