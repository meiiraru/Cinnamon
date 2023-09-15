package mayo;

import mayo.gui.Screen;
import mayo.gui.screens.MainMenu;
import mayo.input.Movement;
import mayo.render.Camera;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Resource;
import mayo.utils.Timer;
import mayo.world.World;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class Client {

    private static final Client INSTANCE = new Client();

    public int windowWidth = 854, windowHeight = 480;
    public int scaledWidth = windowWidth, scaledHeight = windowHeight;
    public float guiScale = 3f;
    private long window;

    private final Timer timer = new Timer(20);
    public int ticks;
    public int fps;
    public int mouseX, mouseY;

    //objects
    public Camera camera;
    public Font font;
    private Screen screen;
    private Movement movement;
    public World world;

    private Client() {}

    public void init(long window) {
        this.window = window;
        this.camera = new Camera();
        this.camera.updateProjMatrix(scaledWidth, scaledHeight);
        this.font = new Font(new Resource("fonts/mayscript.ttf"), 8);
        this.movement = new Movement();
        this.setScreen(null);

        //todo - temp
        this.world = new World();
        world.init();
    }

    public void close() {
        this.font.free();
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

        //render world
        if (world != null) {
            world.render(matrices, delta);
            //finish world rendering
            VertexConsumer.finishAllBatches(camera.getPerspectiveMatrix(), camera.getViewMatrix(delta));
        }

        //render hud
        if (world != null) {
            glClear(GL_DEPTH_BUFFER_BIT); //top of world
            world.renderHUD(matrices, delta);
            VertexConsumer.finishAllBatches(camera.getOrthographicMatrix(), new Matrix4f());
        }

        //render gui
        if (this.screen != null) {
            glClear(GL_DEPTH_BUFFER_BIT); //top of hud
            screen.render(matrices, delta);
            VertexConsumer.finishAllBatches(camera.getOrthographicMatrix(), new Matrix4f());
        }

        //finish rendering
        matrices.pop();
    }

    private void tick() {
        ticks++;

        movement.tick(camera);

        if (world != null)
            world.tick();

        if (screen != null)
            screen.tick();
    }

    public void setScreen(Screen s) {
        //close previous screen
        if (screen != null)
            screen.close();

        //set the screen
        this.screen = s;

        if (s != null) {
            //init the new screen
            s.init(this, scaledWidth, scaledHeight);

            //unlock cursor
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

            //move cursor to the window's center
            glfwSetCursorPos(window, windowWidth / 2f, windowHeight / 2f);
        } else {
            //lock cursor
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }
    }

    // -- glfw events -- //

    public void mousePress(int button, int action, int mods) {
        if (screen != null) screen.mousePress(button, action, mods);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        movement.keyPress(key, action);
        if (screen != null) screen.keyPress(key, scancode, action, mods);

        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_F1)
            this.setScreen(this.screen == null ? new MainMenu() : null);
    }

    public void charTyped(char c, int mods) {
        if (screen != null) screen.charTyped(c, mods);
    }

    public void mouseMove(double x, double y) {
        movement.mouseMove(x, y);
        mouseX = (int) (x / guiScale);
        mouseY = (int) (y / guiScale);

        if (screen != null) screen.mouseMove(x, y);
    }

    public void scroll(double x, double y) {
        guiScale += (float) (Math.signum(y) * 0.1f);
        windowResize(windowWidth, windowHeight);

        if (screen != null) screen.scroll(x, y);
    }

    public void windowResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.scaledWidth = (int) (width / guiScale);
        this.scaledHeight = (int) (height / guiScale);

        if (camera != null)
            camera.updateProjMatrix(scaledWidth, scaledHeight);

        if (screen != null)
            screen.resize(scaledWidth, scaledHeight);
    }

    public void windowFocused(boolean focused) {
        if (screen != null) screen.windowFocused(focused);
    }
}