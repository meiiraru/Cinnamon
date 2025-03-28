package cinnamon;

import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.utils.Resource;
import cinnamon.vr.XrManager;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Platform;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Cinnamon {

    //window settings
    public static int WIDTH = 854, HEIGHT = 480;
    public static String TITLE = "Cinnamon";
    public static Resource ICON = new Resource("textures/icon.png");
    public static Platform PLATFORM = Platform.get();

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
        if (PLATFORM == Platform.MACOSX)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        //create window
        int width = WIDTH;
        int height = HEIGHT;
        long window = glfwCreateWindow(width, height, TITLE, NULL, NULL);
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
            width = Math.min(width, ww);
            height = Math.min(height, wh);
            glfwSetWindowSize(window, width, height);
            glfwSetWindowPos(window, (ww - width) / 2, (wh - height) / 2);
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

        // -- client init -- //

        client.window = new Window(window, width, height);
        client.window.setIcon(ICON);
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

            //tick client
            int ticksToUpdate = client.timer.update();
            for (int j = 0; j < Math.min(20, ticksToUpdate); j++)
                client.tick();

            //render client
            if (XrManager.shouldRender()) {
                XrManager.render(() -> client.render(matrices));
            } else {
                Framebuffer.DEFAULT_FRAMEBUFFER.useClear();
                Framebuffer.DEFAULT_FRAMEBUFFER.adjustViewPort();
                client.render(matrices);
            }

            //end render
            Blit.copy(Framebuffer.DEFAULT_FRAMEBUFFER, 0, PostProcess.BLIT);
            glfwSwapBuffers(window);

            if (!matrices.isEmpty()) {
                LOGGER.error("Forgot to pop the matrix stack! - Popping it for you!");
                while (!matrices.isEmpty()) matrices.pop();
            }
        }
    }

    private void close() {
        glFinish();

        //close client
        Client client = Client.getInstance();
        client.close();
        LOGGER.info("Now leaving... bye!");

        //close xr
        XrManager.close();

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
