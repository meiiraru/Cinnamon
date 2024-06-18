package mayo;

import mayo.logger.LoggerConfig;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.framebuffer.Blit;
import mayo.render.framebuffer.Framebuffer;
import mayo.render.shader.PostProcess;
import mayo.utils.Resource;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static mayo.Client.LOGGER;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {

    public static void main(String[] args) {
        //System.load("D:\\apps\\RenderDoc_1.32_64\\renderdoc.dll");
        new Main().run();
    }

    public static final int WIDTH = 854, HEIGHT = 480;

    private Client client;
    private long window;

    public void run() {
        this.client = Client.getInstance();
        init();
        loop();
        close();
    }

    private void init() {
        //error callback
        GLFWErrorCallback.createPrint(System.err).set();

        //init GLFW
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        //configure GLFW
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        //enable only if were on apple
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("mac"))
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        //create window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Mayonnaise", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");
        client.window = new Window(window, WIDTH, HEIGHT);
        client.window.setIcon(new Resource("textures/icon.png"));

        //input callbacks
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> client.keyPress(key, scancode, action, mods));
        glfwSetCharModsCallback(window, (window, key, mods) -> {for (char c : Character.toChars(key)) client.charTyped(c, mods);});
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> client.mousePress(button, action, mods));
        glfwSetCursorPosCallback(window, (window, x, y) -> client.mouseMove(x, y));
        glfwSetScrollCallback(window, (window, x, y) -> client.scroll(x, y));
        glfwSetWindowPosCallback(window, (window, x, y) -> client.windowMove(x, y));
        glfwSetWindowSizeCallback(window, (window, width, height) -> client.windowResize(width, height));
        glfwSetWindowFocusCallback(window, (window, focused) -> client.windowFocused(focused));

        //make the OpenGL context current
        glfwMakeContextCurrent(window);

        //finishes the initializing process
        GL.createCapabilities();

        // -- init -- //

        //initiate logger
        LoggerConfig.initialize(LOGGER);

        //opengl debug info
        LOGGER.info("OS: {}", os);
        LOGGER.info("Renderer: {}", glGetString(GL_RENDERER));
        LOGGER.info("OpenGL Version: {}", glGetString(GL_VERSION));
        LOGGER.info("LWJGL Version: {}", Version.getVersion());

        //window properties

        //vsync
        glfwSwapInterval(GLFW_FALSE);

        //center window on screen
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(window, (vidMode.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2);
        } else { //could not find video mode, so just maximize the window
            glfwMaximizeWindow(window);
        }

        //and then show the window
        glfwShowWindow(window);

        //finish init by initializing the client
        client.init();
    }

    private void loop() {
        //transform matrix
        MatrixStack matrices = new MatrixStack();

        //flags
        glClearColor(1f, 1f, 1f, 1f);
        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glLineWidth(2.5f);

        //fps count
        double prevSecond = glfwGetTime();
        double prevTime = prevSecond;
        int fps = 0;

        //render loop
        while (!glfwWindowShouldClose(window)) {
            //fps counter
            double currentTime = glfwGetTime();
            fps++;
            if (currentTime - prevSecond >= 1) {
                client.fps = fps;
                client.ms = (int) ((currentTime - prevTime) * 1000);
                fps = 0;
                prevSecond = currentTime;
            }
            prevTime = currentTime;

            //process input events
            glfwPollEvents();

            //render client
            Framebuffer.DEFAULT_FRAMEBUFFER.useClear();
            Framebuffer.DEFAULT_FRAMEBUFFER.adjustViewPort();
            client.render(matrices);
            Blit.copy(Framebuffer.DEFAULT_FRAMEBUFFER, 0, PostProcess.BLIT);

            //end render
            glfwSwapBuffers(window);

            if (!matrices.isEmpty()) {
                LOGGER.error("Forgot to pop the matrix stack! - Popping it for you!");
                while (!matrices.isEmpty()) matrices.pop();
            }
        }
    }

    private void close() {
        //close client
        client.close();

        //free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        //terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

        //close program
        System.exit(0);
    }
}
