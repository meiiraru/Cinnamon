package cinnamon;

import cinnamon.events.Await;
import cinnamon.events.EventType;
import cinnamon.events.Events;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.screens.MainMenu;
import cinnamon.gui.screens.world.PauseScreen;
//import cinnamon.networking.ServerConnection;
import cinnamon.logger.Logger;
import cinnamon.logger.LoggerConfig;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.settings.Settings;
import cinnamon.sound.SoundManager;
import cinnamon.text.Text;
import cinnamon.utils.TextureIO;
import cinnamon.utils.Timer;
import cinnamon.utils.Version;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.Hud;
import cinnamon.world.world.WorldClient;
import org.lwjgl.system.Platform;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Client {

    private static final Client INSTANCE = new Client();
    public static final Logger LOGGER = new Logger(Client.class);

    private final Queue<Runnable> scheduledTicks = new LinkedList<>();

    public static final int TPS = 20;
    public final Timer timer = new Timer(TPS);
    public long ticks;
    public int fps, ms;

    public String name = "Player" + (int) (Math.random() * 1000);
    public UUID playerUUID = UUID.nameUUIDFromBytes(name.getBytes());

    public boolean debug;
    public int postProcess = -1;
    public boolean anaglyph3D = false;

    //events
    public Events events = new Events();
    public Supplier<Screen> mainScreen = MainMenu::new;

    //objects
    public Window window;
    public Camera camera = new Camera();
    public Screen screen;
    public WorldClient world;

    private Client() {}

    public void init() {
        //initiate logger
        LoggerConfig.initialize();

        //opengl debug info
        LOGGER.info("Welcome to Cinnamon! v%s", Version.CLIENT_VERSION);
        LOGGER.info("OS: %s %s", Cinnamon.PLATFORM.getName(), Platform.getArchitecture().name());
        LOGGER.info("Renderer: %s", glGetString(GL_RENDERER));
        LOGGER.info("OpenGL Version: %s", glGetString(GL_VERSION));
        LOGGER.info("LWJGL Version: %s", org.lwjgl.Version.getVersion());

        //init settings
        Settings.load();

        //queue window first update
        this.windowResize(window.width, window.height);

        //init sounds
        SoundManager.init(Settings.soundDevice.get());

        //init open xr
        //XrManager.init(window.getHandle());

        //register and run init events
        events.registerClientEvents();
        events.runEvents(EventType.RESOURCE_INIT);
        events.runEvents(EventType.CLIENT_INIT);

        //open main menu
        this.setScreen(mainScreen.get());
    }

    public void close() {
        //disconnect();
        if (screen != null) screen.removed();
        SoundManager.free();
        events.runEvents(EventType.RESOURCE_FREE);
    }

    public static Client getInstance() {
        return INSTANCE;
    }

    // -- events -- //

    public void tick() {
        ticks++;

        runScheduledTicks();
        //ServerConnection.tick();
        Await.tick();

        SoundManager.tick(camera);

        events.runEvents(EventType.TICK_BEFORE_WORLD);

        if (world != null)
            world.tick();

        events.runEvents(EventType.TICK_BEFORE_GUI);

        if (screen != null)
            screen.tick();

        events.runEvents(EventType.TICK_END);
    }

    private void runScheduledTicks() {
        Runnable toRun;
        while ((toRun = scheduledTicks.poll()) != null)
            toRun.run();
    }

    public void queueTick(Runnable toRun) {
        scheduledTicks.add(toRun);
    }

    public void render(MatrixStack matrices) {
        float delta = timer.partialTick;

        matrices.pushMatrix();

        //run render events
        events.runEvents(EventType.RENDER_BEFORE_WORLD);

        //render world
        if (world != null) {
            world.render(matrices, delta);

            if (!world.hideHUD()) {
                //render first person hand
                if (!world.isThirdPerson()) {
                    glClear(GL_DEPTH_BUFFER_BIT); //top of world
                    world.renderHand(camera, matrices, delta);
                }

                //render world hud
                glClear(GL_DEPTH_BUFFER_BIT); //top of hand

                matrices.pushMatrix();
                if (XrManager.isInXR())
                    XrRenderer.applyGUITransform(matrices);

                world.hud.render(matrices, delta);

                matrices.popMatrix();
            }
        }

        //top of world hud
        glClear(GL_DEPTH_BUFFER_BIT);

        //xr GUI transform
        if (XrManager.isInXR()) {
            XrRenderer.renderHands(matrices);
            XrRenderer.applyGUITransform(matrices);
        }

        //run gui events
        events.runEvents(EventType.RENDER_BEFORE_GUI);

        //render screen
        if (this.screen != null)
            screen.render(matrices, window.mouseX, window.mouseY, delta);

        //render toasts
        if (world == null || !world.hideHUD())
            Toast.renderToasts(matrices, window.getGUIWidth(), window.getGUIHeight(), delta);

        //finish hud
        VertexConsumer.finishAllBatches(camera);

        //apply global post process
        if (postProcess != -1)
            PostProcess.apply(PostProcess.EFFECTS[postProcess]);

        //run post render events
        events.runEvents(EventType.RENDER_END);

        //debug hud always on top
        Hud.renderDebug(matrices);
        VertexConsumer.finishAllBatches(camera);

        //finish rendering
        matrices.popMatrix();
    }

    public void setScreen(Screen s) {
        //remove previous screen
        if (screen != null)
            screen.removed();

        //set the screen
        this.screen = s;

        if (s != null) {
            //init the new screen
            s.init(this, window.getGUIWidth(), window.getGUIHeight());

            //screen has been added
            screen.added();
            LOGGER.debug("Set screen: %s", s.getClass().getSimpleName());

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
            this.camera.reset();
            Toast.clear(Toast.ToastType.WORLD);
            this.setScreen(mainScreen.get());
        });
    }

    public void reloadAssets() {
        queueTick(() -> {
            events.runEvents(EventType.RESOURCE_FREE);
            events.runEvents(EventType.RESOURCE_INIT);
            Toast.clearAll();
            Toast.addToast(Text.translated("debug.assets_reloaded"));
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

        events.runEvents(EventType.MOUSE_PRESS, button, action, mods);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            switch (key) {
                case GLFW_KEY_F2 -> {
                    TextureIO.screenshot(window.width, window.height);
                    Toast.addToast(Text.of("Screenshot Taken!"));
                }
                case GLFW_KEY_F3 -> debug = !debug;
                case GLFW_KEY_F9 -> {
                    boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
                    if (postProcess == -1)
                        postProcess = shift ? PostProcess.EFFECTS.length - 1 : 0;
                    else if ((postProcess == 0 && shift) || (postProcess == PostProcess.EFFECTS.length - 1 && !shift))
                        postProcess = -1;
                    else
                        postProcess += shift ? -1 : 1;
                }
                case GLFW_KEY_F10 -> anaglyph3D = !anaglyph3D;
                case GLFW_KEY_F11 -> window.toggleFullScreen();
                case GLFW_KEY_ENTER -> {if ((mods & GLFW_MOD_ALT) != 0) window.toggleFullScreen();}
                case GLFW_KEY_F12 -> {
                    boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
                    if (shift) {
                        queueTick(() -> {
                            if (screen != null)
                                screen.rebuild();
                        });
                    } else {
                        reloadAssets();
                    }
                }
            }
        }

        if (screen != null) {
            screen.keyPress(key, scancode, action, mods);
        } else if (world != null && window.isMouseLocked()) {
            world.keyPress(key, scancode, action, mods);
        }

        events.runEvents(EventType.KEY_PRESS, key, scancode, action, mods);
    }

    public void charTyped(char c, int mods) {
        if (screen != null)
            screen.charTyped(c, mods);

        events.runEvents(EventType.CHAR_TYPED, c, mods);
    }

    public void mouseMove(double x, double y) {
        window.updateMosuePos(x, y);

        if (screen != null) {
            screen.mouseMove(window.mouseX, window.mouseY);
        } else if (world != null && window.isMouseLocked()) {
            world.mouseMove(x, y);
        }

        events.runEvents(EventType.MOUSE_MOVE, x, y);
    }

    public void scroll(double x, double y) {
        if (screen != null)
            screen.scroll(x, y);
        else if (world != null && window.isMouseLocked())
            world.scroll(x, y);

        events.runEvents(EventType.SCROLL, x, y);
    }

    public void windowMove(int x, int y) {
        window.updatePos(x, y);
        events.runEvents(EventType.WINDOW_MOVE, x, y);
    }

    public void windowResize(int width, int height) {
        if (width <= 0 || height <= 0)
            return;

        window.updateSize(width, height, Settings.guiScale.get());
        Framebuffer.DEFAULT_FRAMEBUFFER.resize(width, height);

        if (world != null)
            world.onWindowResize(width, height);

        queueTick(() -> {
            if (camera != null)
                camera.updateProjMatrix(window.scaledWidth, window.scaledHeight, Settings.fov.get());

            if (screen != null) {
                if (XrManager.isInXR())
                    screen.rebuild();
                else
                    screen.resize(window.scaledWidth, window.scaledHeight);
            }
        });

        events.runEvents(EventType.WINDOW_RESIZE, width, height);
    }

    public void windowFocused(boolean focused) {
        if (screen != null) {
            screen.windowFocused(focused);
        } else if (world != null && !focused) {
            this.setScreen(new PauseScreen());
        }

        events.runEvents(EventType.WINDOW_FOCUSED, focused);
    }

    public void filesDropped(String[] files) {
        if (screen != null)
            screen.filesDropped(files);

        events.runEvents(EventType.FILES_DROPPED, (Object) files);
    }
}