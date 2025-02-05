package cinnamon;

import cinnamon.logger.LoggerConfig;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Cinnamon {

    public static void main(String[] args) {
        //System.load("D:\\apps\\RenderDoc_1.32_64\\renderdoc.dll");
        new Cinnamon().run();
    }

    public void run() {
        init();
        loop();
        close();
    }

    private void init() {
        Client client = Client.getInstance();

        //error callback
        GLFWErrorCallback.createPrint(System.err).set();

        //init GLFW
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        //configure GLFW
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        //enable only if we are on apple
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("mac"))
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        //create window
        int width = client.windowSettings.defaultWidth;
        int height = client.windowSettings.defaultHeight;
        long window = glfwCreateWindow(width, height, client.windowSettings.title, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        //make the OpenGL context current
        glfwMakeContextCurrent(window);

        //vsync
        glfwSwapInterval(GLFW_FALSE);

        //set window size and position
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            int ww = vidMode.width();
            int wh = vidMode.height();
            width = (int) (ww * 0.45f);
            height = (int) (wh * 0.45f);
            glfwSetWindowSize(window, width, height);
            glfwSetWindowPos(window, (ww - width) / 2, (wh - height) / 2);
        } else { //could not find video mode, so just maximize the window
            glfwMaximizeWindow(window);
        }

        //and then show the window
        glfwShowWindow(window);

        //register input callbacks
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> client.keyPress(key, scancode, action, mods));
        glfwSetCharModsCallback(window, (win, key, mods) -> {
            for (char c : Character.toChars(key))
                client.charTyped(c, mods);
        });
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> client.mousePress(button, action, mods));
        glfwSetCursorPosCallback(window, (win, x, y) -> client.mouseMove(x, y));
        glfwSetScrollCallback(window, (win, x, y) -> client.scroll(x, y));
        glfwSetWindowPosCallback(window, (win, x, y) -> client.windowMove(x, y));
        glfwSetWindowSizeCallback(window, (win, w, h) -> client.windowResize(w, h));
        glfwSetWindowFocusCallback(window, (win, focused) -> client.windowFocused(focused));
        glfwSetDropCallback(window, (win, count, names) -> {
            String[] nameArray = new String[count];
            for (int i = 0; i < count; i++)
                nameArray[i] = GLFWDropCallback.getName(names, i);
            client.filesDropped(nameArray);
        });

        //finishes the initialization process
        GL.createCapabilities();

        // -- init -- //

        //initiate logger
        LoggerConfig.initialize(LOGGER);

        //opengl debug info
        LOGGER.info("Welcome to Cinnamon!");
        LOGGER.info("OS: %s", os);
        LOGGER.info("Renderer: %s", glGetString(GL_RENDERER));
        LOGGER.info("OpenGL Version: %s", glGetString(GL_VERSION));
        LOGGER.info("LWJGL Version: %s", Version.getVersion());

        //finish init through the client
        client.window = new Window(window, width, height);
        client.init();
    }

    private void loop() {
        Client client = Client.getInstance();
        long window = client.window.getHandle();

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
        Client client = Client.getInstance();

        //close client
        client.close();
        LOGGER.info("Now leaving... bye!");

        //free the window callbacks and destroy the window
        long window = client.window.getHandle();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        //terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

        //close program
        System.exit(0);
    }
}
