package mayo;

import mayo.render.BatchRenderer;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.utils.Timer;

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
    private final Camera camera;

    private Client() {
        this.camera = new Camera(windowWidth / (float) windowHeight);
    }

    public void init() {
    }

    public void close() {
    }

    public static Client getInstance() {
        return INSTANCE;
    }

    // -- events -- //

    public void render(BatchRenderer renderer, MatrixStack stack) {
        //tick
        int ticksToUpdate = timer.update();
        for (int j = 0; j < Math.min(10, ticksToUpdate); j++)
            tick();
    }

    private void tick() {
        ticks++;
    }

    // -- glfw events -- //

    public void mousePress(int button, int action, int mods) {
    }

    public void keyPress(int key, int scancode, int action, int mods) {
    }

    public void charTyped(char c, int mods) {
    }

    public void mouseMove(double x, double y) {
    }

    public void scroll(double x, double y) {
        guiScale += (float) (Math.signum(y) * 0.1f);
        windowResize(windowWidth, windowHeight);
    }

    public void windowResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        this.scaledWidth = (int) (width / guiScale);
        this.scaledHeight = (int) (height / guiScale);
        this.camera.updatePerspective(windowWidth / (float) windowHeight);
    }

    public void windowFocused(boolean focused) {
    }

    public Camera camera() {
        return this.camera;
    }
}