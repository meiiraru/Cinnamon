package mayo;

import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.screens.MainMenu;
import mayo.gui.screens.PauseScreen;
import mayo.render.Camera;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Resource;
import mayo.utils.Timer;
import mayo.world.World;
import org.joml.Matrix4f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class Client {

    private static final Client INSTANCE = new Client();

    private final Timer timer = new Timer(20);
    public int ticks;
    public int fps;

    //objects
    public Window window;
    public Camera camera;
    public Font font;
    public Screen screen;
    public World world;

    private Client() {}

    public void init() {
        this.camera = new Camera();
        this.camera.updateProjMatrix(this.window.scaledWidth, this.window.scaledHeight);
        this.font = new Font(new Resource("fonts/mayscript.ttf"), 8);
        this.setScreen(new MainMenu());
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

        if (world != null) {
            //render world
            world.render(matrices, delta);
            //finish world rendering
            VertexConsumer.finishAllBatches(camera.getPerspectiveMatrix(), camera.getViewMatrix());

            //render hud
            glClear(GL_DEPTH_BUFFER_BIT); //top of world
            world.renderHUD(matrices, delta);
            VertexConsumer.finishAllBatches(camera.getOrthographicMatrix(), new Matrix4f());
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

        if (world != null)
            world.tick();

        if (screen != null)
            screen.tick();
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
            //no screen, then lock the mouse
            //reset movement
            world.resetMovement();
        }
    }

    // -- glfw events -- //

    public void mousePress(int button, int action, int mods) {
        if (screen != null)
            screen.mousePress(button, action, mods);
        else if (world != null)
            world.mousePress(button, action, mods);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && key == GLFW_KEY_F11)
            window.toggleFullScreen();

        if (screen != null) {
            screen.keyPress(key, scancode, action, mods);
        } else if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) {
            this.setScreen(new PauseScreen());
        } else if (world != null) {
            world.keyPress(key, scancode, action, mods);
        }
    }

    public void charTyped(char c, int mods) {
        if (screen != null) screen.charTyped(c, mods);
    }

    public void mouseMove(double x, double y) {
        window.mouseMove(x, y);

        if (screen != null) {
            screen.mouseMove(x, y);
        } else if (world != null) {
            world.mouseMove(x, y);
        }
    }

    public void scroll(double x, double y) {
        if (screen != null) screen.scroll(x, y);
    }

    public void windowMove(int x, int y) {
        window.windowMove(x, y);
    }

    public void windowResize(int width, int height) {
        window.windowResize(width, height);

        if (camera != null)
            camera.updateProjMatrix(window.scaledWidth, window.scaledHeight);

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