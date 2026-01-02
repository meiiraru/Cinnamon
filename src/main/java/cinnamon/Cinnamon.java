package cinnamon;

import cinnamon.render.Window;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.settings.ArgsOptions;
import cinnamon.settings.Settings;
import cinnamon.utils.FileDialog;
import cinnamon.utils.Resource;
import cinnamon.vr.XrManager;
import org.joml.Math;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Platform;

import java.util.HashSet;
import java.util.Set;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS;
import static org.lwjgl.opengl.GL30.glGetStringi;
import static org.lwjgl.opengl.GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Cinnamon {

    //window settings
    public static int WIDTH = 854, HEIGHT = 480;
    public static String TITLE = "Cinnamon";
    public static String NAMESPACE = "cinnamon";
    public static Resource ICON = new Resource("textures/icon.png");
    public static boolean ENABLE_XR = false;

    //system properties
    public static final Platform PLATFORM = Platform.get();
    public static final Set<String> OPENGL_EXTENSIONS = new HashSet<>();

    public static void main(String... args) {
        new Cinnamon(args).run();
    }

    public Cinnamon(String... args) {
        //parse command line arguments
        ArgsOptions.parse(args);

        //render doc
        String doc = ArgsOptions.RENDER_DOC.getAsString();
        if (!doc.isBlank())
            System.load(doc);
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

        //glfw platform
        String glfwPlatform = ArgsOptions.FORCE_GLFW_PLATFORM.getAsString();
        if (!glfwPlatform.isBlank()) {
            try {
                glfwInitHint(GLFW_PLATFORM, GLFW.class.getField("GLFW_PLATFORM_" + glfwPlatform.toUpperCase()).getInt(null));
            } catch (Exception ignored) {}
        }

        //init GLFW
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        //configure GLFW
        if (ArgsOptions.EXPERIMENTAL_OPENGL_ES.getAsBool()) {
            glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        } else {
            glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        }
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

        //save detected extensions
        int numExt = glGetInteger(GL_NUM_EXTENSIONS);
        for (int i = 0; i < numExt; i++)
            OPENGL_EXTENSIONS.add(glGetStringi(GL_EXTENSIONS, i));

        // -- client init -- //

        client.window = new Window(window, width, height);
        client.window.setIcon(ICON);
        client.init();
    }

    private void loop() {
        Client client = Client.getInstance();
        long window = client.window.getHandle();

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
        int fps = 0;
        int ms = 0;

        //render loop
        while (!glfwWindowShouldClose(window)) {
            double frameStartTime = glfwGetTime();

            //fps counter
            if (frameStartTime - prevSecond >= 1) {
                client.fps = fps;
                client.ms = ms / fps; //average ms per second
                fps = ms = 0;
                prevSecond = frameStartTime;
            }

            //process input events
            glfwPollEvents();

            //tick client
            int ticksToUpdate = client.timer.update();
            for (int j = 0; j < Math.min(20, ticksToUpdate); j++)
                client.tick();

            //render client
            if (!XrManager.render(client::render)) {
                Framebuffer.DEFAULT_FRAMEBUFFER.useClear();
                Framebuffer.DEFAULT_FRAMEBUFFER.adjustViewPort();
                client.render();
            }

            //end render
            PostProcess.finishFrame();
            Framebuffer.DEFAULT_FRAMEBUFFER.blit(0);
            glfwSwapBuffers(window);

            //ms counter
            double frameEndTime = glfwGetTime();
            double frameDuration = frameEndTime - frameStartTime;
            ms += (int) (frameDuration * 1000);
            fps++;

            //limit fps
            int frameCap = Settings.fpsLimit.get();
            if (frameCap > 0 && frameDuration < 1d / frameCap) {
                try {
                    Thread.sleep((long) ((1d / frameCap - frameDuration) * 1000));
                } catch (InterruptedException e) {
                    LOGGER.warn("Thread interrupted while sleeping to limit FPS", e);
                }
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

        //close the file dialog
        FileDialog.close();

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
