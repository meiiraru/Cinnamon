package mayo;

import mayo.gui.Screen;
import mayo.gui.screens.MainMenu;
import mayo.input.Movement;
import mayo.render.BatchRenderer;
import mayo.render.Camera;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.utils.Timer;
import mayo.world.World;

public class Client {

    public static final String NAMESPACE = "mayo";
    private static final Client INSTANCE = new Client();

    public int windowWidth = 854, windowHeight = 480;
    public int scaledWidth = windowWidth, scaledHeight = windowHeight;
    public float guiScale = 3f;

    private final Timer timer = new Timer(20);
    public int ticks;
    public int fps;

    //objects
    public Camera camera;
    public Font font;
    public Screen screen;
    private Movement movement;
    public World world;

    private Client() {}

    public void init() {
        this.camera = new Camera();
        this.camera.updateProjMatrix(scaledWidth, scaledHeight);
        this.font = new Font(Client.NAMESPACE, "mayscript", 8);
        this.screen = new MainMenu();
        this.movement = new Movement();
    }

    public void close() {
        this.font.free();
    }

    public static Client getInstance() {
        return INSTANCE;
    }

    // -- events -- //

    public void render(BatchRenderer renderer, MatrixStack matrices) {
        //tick
        int ticksToUpdate = timer.update();
        for (int j = 0; j < Math.min(10, ticksToUpdate); j++)
            tick();

        float delta = timer.delta();

        //render world
        if (world != null) {
            matrices.peek().set(camera.getPerspectiveMatrix()).mul(camera.getViewMatrix());
            world.render(renderer, matrices, delta);
        }

        //render hud
        matrices.peek().set(camera.getOrthographicMatrix());

        if (world != null)
            world.renderHUD(renderer, matrices, delta);

        //render gui
        if (this.screen != null)
            screen.render(renderer, matrices, delta);
    }

    private void tick() {
        ticks++;

        movement.tick(camera);

        if (world != null)
            world.tick();

        if (screen != null)
            screen.tick();
    }

    // -- glfw events -- //

    public void mousePress(int button, int action, int mods) {
        if (screen != null) screen.mousePress(button, action, mods);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        movement.keyPress(key, action);
        if (screen != null) screen.keyPress(key, scancode, action, mods);
    }

    public void charTyped(char c, int mods) {
        if (screen != null) screen.charTyped(c, mods);
    }

    public void mouseMove(double x, double y) {
        movement.mouseMove(x, y);
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
            camera.updateProjMatrix(width, height);

        if (screen != null)
            screen.windowResize(width, height);
    }

    public void windowFocused(boolean focused) {
        if (screen != null) screen.windowFocused(focused);
    }
}