package mayo.render;

import mayo.Client;
import mayo.render.shader.Shader;
import mayo.utils.Resource;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public enum PostProcess {

    NONE,
    INVERT,
    BLUR,
    EDGES,
    CHROMATIC_ABERRATION;

    private final Shader shader;

    PostProcess() {
        this.shader = new Shader(new Resource("shaders/post/" + this.name().toLowerCase() + ".glsl"));
    }

    // -- static framebuffer stuff -- //

    public static final Framebuffer frameBuffer;
    private static final int vao, vbo;
    private static final float[] plane = {
            //x    y     u   v
            -1f,  1f,    0f, 1f,
            -1f, -1f,    0f, 0f,
             1f, -1f,    1f, 0f,

            -1f,  1f,    0f, 1f,
             1f, -1f,    1f, 0f,
             1f,  1f,    1f, 1f
    };

    static {
        Window w = Client.getInstance().window;
        frameBuffer = new Framebuffer(w.width, w.height);
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        glBufferData(GL_ARRAY_BUFFER, plane, GL_STATIC_DRAW);

        int stride = Float.BYTES * 4;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, Float.BYTES * 2);
    }

    public static void prepare() {
        frameBuffer.use();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public static void render(PostProcess effect) {
        //use framebuffer
        Framebuffer.useDefault();

        //prepare shader
        Shader s = effect.shader.use();
        s.setVec2("textelSize", 1f / frameBuffer.getWidth(), 1f / frameBuffer.getHeight());

        //textures
        s.setInt("screenTexture", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, frameBuffer.getColorBuffer());

        s.setInt("depthTexture", 1);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, frameBuffer.getDepthStencilBuffer());

        //disable depth
        glDisable(GL_DEPTH_TEST);

        //draw
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);

        //re-enable depth test
        glEnable(GL_DEPTH_TEST);
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
