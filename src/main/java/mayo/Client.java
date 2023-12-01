package mayo;

import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.screens.MainMenu;
import mayo.gui.screens.PauseScreen;
import mayo.networking.ClientConnection;
import mayo.networking.ServerConnection;
import mayo.options.Options;
import mayo.render.Camera;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.render.framebuffer.PostProcess;
import mayo.resource.ResourceManager;
import mayo.sound.SoundManager;
import mayo.utils.Resource;
import mayo.utils.Timer;
import mayo.world.WorldClient;
import org.joml.Matrix4f;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F11;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class Client {

    private static final Client INSTANCE = new Client();
    public static final String VERSION = "0.1";
    public static final String PLAYERNAME = String.valueOf(System.currentTimeMillis());
    public static final UUID PLAYER_UUID = UUID.nameUUIDFromBytes(PLAYERNAME.getBytes());

    private final Queue<Runnable> scheduledTicks = new LinkedList<>();

    private final Timer timer = new Timer(20);
    public int ticks;
    public int fps;

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
        this.window.setGuiScale(options.guiScale);
        this.soundManager = new SoundManager();
        this.soundManager.init();
        this.camera = new Camera();
        this.camera.updateProjMatrix(this.window.scaledWidth, this.window.scaledHeight, this.options.fov);
        this.font = new Font(new Resource("fonts/mayscript.ttf"), 8);
        ResourceManager.register();
        ResourceManager.init();
        this.setScreen(new MainMenu());
    }

    public void close() {
        disconnect();
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
        for (int j = 0; j < Math.min(10, ticksToUpdate); j++)
            tick();

        float delta = timer.delta();

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
        Toast.renderToasts(matrices, window.scaledWidth, window.scaledHeight, delta);

        VertexConsumer.finishAllBatches(camera.getOrthographicMatrix(), new Matrix4f());

        //finish rendering
        matrices.pop();
    }

    private void tick() {
        ticks++;

        runScheduledTicks();

        soundManager.tick(camera);

        if (world != null)
            world.tick();

        if (screen != null)
            screen.tick();

        ServerConnection.tick();
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
        } else if (window.lockMouse() && world != null) {
            //no screen, then lock the mouse and reset player movement
            world.resetMovement();
        }
    }

    public void disconnect() {
        ClientConnection.disconnect();

        queueTick(() -> {
            if (this.world != null) {
                this.world.close();
                this.world = null;
            }
        });

        this.setScreen(new MainMenu());
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
        if (action == GLFW_PRESS && key == GLFW_KEY_F11)
            window.toggleFullScreen();

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
        if (width > 0 && height > 0) {
            window.windowResize(width, height);
            PostProcess.resize(width, height);
        }

        if (camera != null)
            camera.updateProjMatrix(window.scaledWidth, window.scaledHeight, this.options.fov);

        if (screen != null)
            screen.resize(window.scaledWidth, window.scaledHeight);
    }

    public void windowFocused(boolean focused) {
        if (screen != null) {
            screen.windowFocused(focused);
        } else if (world != null && !focused) {
            this.setScreen(new PauseScreen());
        }
    }
}