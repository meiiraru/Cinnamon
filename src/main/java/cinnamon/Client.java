package cinnamon;

import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.screens.MainMenu;
import cinnamon.gui.screens.world.PauseScreen;
//import cinnamon.networking.ServerConnection;
import cinnamon.options.Options;
import cinnamon.render.Camera;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.resource.ResourceManager;
import cinnamon.sound.SoundManager;
import cinnamon.text.Text;
import cinnamon.utils.Await;
import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import cinnamon.utils.Timer;
import cinnamon.world.world.WorldClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class Client {

    private static final Client INSTANCE = new Client();
    public static final String VERSION = "0.0.9";
    public static final Logger LOGGER = LogManager.getLogger(Main.class);

    private final Queue<Runnable> scheduledTicks = new LinkedList<>();

    public static final int TPS = 20;
    public final Timer timer = new Timer(TPS);
    public long ticks;
    public int fps, ms;

    public String name = "Meii";
    public UUID playerUUID = UUID.nameUUIDFromBytes(name.getBytes());

    //objects
    public SoundManager soundManager;
    public Window window;
    public Options options;
    public Camera camera;
    public Font font;
    public Screen screen;
    public WorldClient world;

    private Client() {}

    public void init() {
        this.options = Options.load();
        this.windowResize(window.width, window.height);
        this.soundManager = new SoundManager();
        this.soundManager.init();
        this.camera = new Camera();
        this.camera.updateProjMatrix(this.window.scaledWidth, this.window.scaledHeight, this.options.fov);
        this.font = new Font(new Resource("fonts/mayonnaise.ttf"), 8);
        ResourceManager.register();
        ResourceManager.init();
        this.setScreen(new MainMenu());
    }

    public void close() {
        //disconnect();
        this.font.free();
        this.soundManager.free();
        ResourceManager.free();
    }

    public static Client getInstance() {
        return INSTANCE;
    }

    // -- events -- //

    public void render(MatrixStack matrices) {
        //tick
        int ticksToUpdate = timer.update();
        for (int j = 0; j < Math.min(20, ticksToUpdate); j++)
            tick();

        float delta = timer.partialTick;

        matrices.push();

        if (world != null) {
            //render world
            world.render(matrices, delta);

            if (!world.hideHUD()) {
                //render first person hand
                if (!world.isThirdPerson()) {
                    glClear(GL_DEPTH_BUFFER_BIT); //top of world
                    world.renderHand(camera, matrices, delta);
                }

                //render hud
                glClear(GL_DEPTH_BUFFER_BIT); //top of hand
                world.hud.render(matrices, delta);
            }
        }

        //render gui
        glClear(GL_DEPTH_BUFFER_BIT); //top of hud

        if (this.screen != null)
            screen.render(matrices, window.mouseX, window.mouseY, delta);

        //toasts
        if (world == null || !world.hideHUD())
            Toast.renderToasts(matrices, window.scaledWidth, window.scaledHeight, delta);

        VertexConsumer.finishAllBatches(camera.getOrthographicMatrix(), new Matrix4f());

        //finish rendering
        matrices.pop();
    }

    private void tick() {
        ticks++;

        runScheduledTicks();
        //ServerConnection.tick();
        Await.tick();

        soundManager.tick(camera);

        if (world != null)
            world.tick();

        if (screen != null)
            screen.tick();
    }

    private void runScheduledTicks() {
        Runnable toRun;
        while ((toRun = scheduledTicks.poll()) != null)
            toRun.run();
    }

    public void queueTick(Runnable toRun) {
        scheduledTicks.add(toRun);
    }

    public void setScreen(Screen s) {
        //remove previous screen
        if (screen != null)
            screen.removed();

        //set the screen
        this.screen = s;

        if (s != null) {
            //init the new screen
            s.init(this, window.scaledWidth, window.scaledHeight);

            //screen has been added
            screen.added();

            //unlock mouse
            window.unlockMouse();

            //reset player movement
            if (world != null)
                world.resetMovement();
        } else if (window.lockMouse() && world != null) {
            //no screen, then lock the mouse and reset player movement
            world.resetMovement();
        }
    }

    public void disconnect() {
        //ClientConnection.disconnect();
        queueTick(() -> {
            this.world = null;
            this.camera.setEntity(null);
            Toast.clear(Toast.ToastType.WORLD);
            this.setScreen(new MainMenu());
        });
    }

    public void reloadAssets() {
        queueTick(() -> {
            ResourceManager.free();
            ResourceManager.init();
            Toast.clearAll();
            Toast.addToast(Text.of("Reloaded assets"), font);
        });
    }

    public void setName(String name) {
        this.name = name;
        this.playerUUID = UUID.nameUUIDFromBytes(name.getBytes());
    }

    // -- glfw events -- //

    public void mousePress(int button, int action, int mods) {
        window.mousePress(button, action, mods);

        if (screen != null)
            screen.mousePress(button, action, mods);
        else if (world != null && window.isMouseLocked())
            world.mousePress(button, action, mods);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            switch (key) {
                case GLFW_KEY_F2 -> TextureIO.screenshot(window.width, window.height);
                case GLFW_KEY_F11 -> window.toggleFullScreen();
                case GLFW_KEY_F12 -> reloadAssets();
            }
        }

        if (screen != null) {
            screen.keyPress(key, scancode, action, mods);
        } else if (world != null && window.isMouseLocked()) {
            world.keyPress(key, scancode, action, mods);
        }
    }

    public void charTyped(char c, int mods) {
        if (screen != null) screen.charTyped(c, mods);
    }

    public void mouseMove(double x, double y) {
        window.mouseMove(x, y);

        if (screen != null) {
            screen.mouseMove(window.mouseX, window.mouseY);
        } else if (world != null && window.isMouseLocked()) {
            world.mouseMove(x, y);
        }
    }

    public void scroll(double x, double y) {
        if (screen != null)
            screen.scroll(x, y);
        else if (world != null && window.isMouseLocked())
            world.scroll(x, y);
    }

    public void windowMove(int x, int y) {
        window.windowMove(x, y);
    }

    public void windowResize(int width, int height) {
        if (width <= 0 || height <= 0)
            return;

        window.windowResize(width, height, options.guiScale);
        Framebuffer.DEFAULT_FRAMEBUFFER.resize(width, height);

        if (world != null)
            world.onWindowResize(width, height);

        queueTick(() -> {
            if (camera != null)
                camera.updateProjMatrix(window.scaledWidth, window.scaledHeight, this.options.fov);

            if (screen != null)
                screen.resize(window.scaledWidth, window.scaledHeight);
        });
    }

    public void windowFocused(boolean focused) {
        if (screen != null) {
            screen.windowFocused(focused);
        } else if (world != null && !focused) {
            this.setScreen(new PauseScreen());
        }
    }

    public void filesDropped(String[] files) {
        if (screen != null)
            screen.filesDropped(files);
    }
}