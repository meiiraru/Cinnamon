package mayo;

import mayo.render.MatrixStack;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {

    public static void main(String[] args) {
        System.load("D:\\apps\\RenderDoc_1.28_64\\renderdoc.dll");
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
        window = glfwCreateWindow(client.windowWidth, client.windowHeight, "May-o", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        //input callbacks
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> client.keyPress(key, scancode, action, mods));
        glfwSetCharModsCallback(window, (window, key, mods) -> {for (char c : Character.toChars(key)) client.charTyped(c, mods);});
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> client.mousePress(button, action, mods));
        glfwSetCursorPosCallback(window, (window, x, y) -> client.mouseMove(x, y));
        glfwSetScrollCallback(window, (window, x, y) -> client.scroll(x, y));
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
        glClearColor(0.07f, 0.07f, 0.1f, 1f);
        glEnable(GL_CULL_FACE);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        //glLineWidth(5f);

        //timer
        double previousTime = glfwGetTime();

        //render loop
        while (!glfwWindowShouldClose(window)) {
            //fps counter
            double currentTime = glfwGetTime();
            client.fps++;

            if (currentTime - previousTime >= 1) {
                glfwSetWindowTitle(window, "May-o " + client.fps + " fps");
                client.fps = 0;
                previousTime = currentTime;
            }

            glfwPollEvents();

            //clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            //update viewport
            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                glfwGetFramebufferSize(window, w, h);
                glViewport(0, 0, w.get(0), h.get(0));
            }

            //render client
            client.render(matrices);

            //end render
            glfwSwapBuffers(window);

            if (!matrices.isEmpty())
                System.out.println("Forgot to pop the matrix stack!");
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
    }
}
