package mayo;

import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.framebuffer.Blit;
import mayo.render.framebuffer.Framebuffer;
import mayo.render.framebuffer.PostProcess;
import mayo.utils.Resource;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

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
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        //create window
        window = glfwCreateWindow(854, 480, "May~o", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");
        client.window = new Window(window, 854, 480);
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

        //opengl debug info
        System.out.println("Renderer: " + glGetString(GL_RENDERER));
        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("LWJGL Version: " + Version.getVersion());

        //vsync
        glfwSwapInterval(GLFW_FALSE);

        //window properties
        glfwShowWindow(window);
        glfwMaximizeWindow(window);

        //init client
        client.init();
    }

    private void loop() {
        //transform matrix
        MatrixStack matrices = new MatrixStack();

        //flags
        glClearColor(1f, 1f, 1f, 1f);
        //glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glLineWidth(10f);

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
            Blit.copy(Framebuffer.DEFAULT_FRAMEBUFFER, 0, PostProcess.BLIT.getShader());

            //end render
            glfwSwapBuffers(window);

            if (!matrices.isEmpty()) {
                System.err.println("Forgot to pop the matrix stack! - Popping it for you!");
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
