package mayo;

import mayo.gui.Screen;
import mayo.gui.screens.MainMenu;
import mayo.input.Movement;
import mayo.model.obj.Mesh2;
import mayo.parsers.ObjLoader;
import mayo.render.BatchRenderer;
import mayo.render.Camera;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import mayo.utils.Timer;
import mayo.world.World;
import org.lwjgl.glfw.GLFW;

public class Client {

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
        this.font = new Font(new Resource("fonts/mayscript.ttf"), 8);
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

        matrices.push();

        //render world
        if (world != null) {
            matrices.peek().set(camera.getPerspectiveMatrix()).mul(camera.getViewMatrix(delta));
            world.render(renderer, matrices, delta);
        }

        //render hud
        matrices.peek().set(camera.getOrthographicMatrix());

        if (world != null)
            world.renderHUD(renderer, matrices, delta);

        //render gui
        if (this.screen != null)
            screen.render(renderer, matrices, delta);

        matrices.pop();


        // -- TEMP -- //


        if (a) {
            a = false;
            mesh = ObjLoader.load(new Resource("models/teapot.obj")).bake();
            mesh2 = ObjLoader.load(new Resource("models/mesa/mesa01.obj")).bake();
            mesh3 = ObjLoader.load(new Resource("models/bunny.obj")).bake();
            mesh4 = ObjLoader.load(new Resource("models/cube/cube.obj")).bake();
        }

        Shader s = Shaders.MODEL.getShader();
        s.use();
        s.setProjectionMatrix(camera.getPerspectiveMatrix());
        s.setViewMatrix(camera.getViewMatrix(delta));

        //render mesh 1
        matrices.push();
        matrices.translate(0, -mesh.getBBMin().y + (mesh2.getBBMax().y - mesh2.getBBMin().y), 0);
        matrices.scale(0.5f);
        s.setModelMatrix(matrices.peek());
        mesh.render();
        matrices.pop();

        //render mesh 2
        s.setModelMatrix(matrices.peek());
        mesh2.render();

        //render mesh 3
        matrices.push();
        matrices.translate(-3f, (mesh2.getBBMax().y - mesh2.getBBMin().y) - 1f, -4f);
        matrices.rotate(Rotation.Y.rotationDeg(ticks + delta));
        matrices.scale(30f);
        s.setModelMatrix(matrices.peek());
        mesh3.render();
        matrices.pop();

        //render mesh 4
        matrices.push();
        matrices.scale(2f);
        matrices.translate(0, -mesh4.getBBMin().y, 0);
        s.setModelMatrix(matrices.peek());
        mesh4.render();
        matrices.pop();
    }
    private Mesh2 mesh, mesh2, mesh3, mesh4;
    private boolean a = true;

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

        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_F1)
            this.screen = this.screen == null ? new MainMenu() : null;
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
            camera.updateProjMatrix(scaledWidth, scaledHeight);

        if (screen != null)
            screen.windowResize(width, height);
    }

    public void windowFocused(boolean focused) {
        if (screen != null) screen.windowFocused(focused);
    }
}