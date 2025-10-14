package cinnamon.gui.screens.extras;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import org.joml.Math;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class DVDScreen extends ParentedScreen {

    private static final Resource DVD_TEX = new Resource("textures/gui/dvd.png");
    private static final int w = 58, h = 40;
    private static float speed = 2f;

    private static boolean mousePressed = false;
    private static int anchorX, anchorY;
    private static int directionX, directionY;

    //normals
    private static final Vector2f
            UP = new Vector2f(0f, 1f),
            DOWN = new Vector2f(0f, -1f),
            LEFT = new Vector2f(-1f, 0f),
            RIGHT = new Vector2f(1f, 0f);

    private final Vector2f
            oPos = new Vector2f(),
            pos = new Vector2f();
    private float rot = 0;
    private Colors color;

    public DVDScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void added() {
        //fullscreen toast
        Toast.addToast(Text.translated("gui.dvd_screen.help"));

        //set color and position
        this.changeColor();
        pos.set((int) ((width - w) / 2f), (int) ((height - h) / 2f));

        //get a random 45 degrees angle
        rot = (int) (Math.random() * 4) * 90 + 45;
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();
    }

    @Override
    public void tick() {
        super.tick();

        //update position
        oPos.set(pos);

        Vector2f dir = Maths.rotToDir(rot); //already normalized
        pos.add(dir.x * speed, dir.y * speed);

        if (mousePressed)
            pos.set(anchorX - w / 2f, anchorY - h / 2f);

        //up
        if (pos.y <= 0f) {
            dir.set(Maths.reflect(dir, UP));
            changeColor();
            pos.y = 0f;
        }
        //down
        if (pos.y + h >= height) {
            dir.set(Maths.reflect(dir, DOWN));
            changeColor();
            pos.y = height - h;
        }
        //left
        if (pos.x <= 0f) {
            dir.set(Maths.reflect(dir, LEFT));
            changeColor();
            pos.x = 0f;
        }
        //right
        if (pos.x + w >= width) {
            dir.set(Maths.reflect(dir, RIGHT));
            changeColor();
            pos.x = width - w;
        }

        //update rotation
        rot = Maths.dirToRot(dir);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        //super.renderBackground(matrices, delta);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Vector2f p = Maths.lerp(oPos, pos, delta);
        Vertex[] vertices = GeometryHelper.quad(matrices, p.x, p.y, w, h);
        for (Vertex vertex : vertices)
            vertex.color(color.argb);
        VertexConsumer.MAIN.consume(vertices, DVD_TEX);

        if (mousePressed)
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, anchorX, anchorY, anchorX + directionX, anchorY + directionY, 1f, Colors.WHITE.argb));

        //render children on top
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void changeColor() {
        Colors temp = color;
        while (temp == color)
            temp = Colors.randomRainbow();
        this.color = temp;
    }

    @Override
    protected boolean shouldRenderMouse() {
        return false;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (super.mousePress(button, action, mods))
            return true;

        if (button == GLFW_MOUSE_BUTTON_1) {
            mousePressed = action == GLFW.GLFW_PRESS;
            if (mousePressed) {
                Window w = Client.getInstance().window;
                anchorX = w.mouseX;
                anchorY = w.mouseY;
            } else {
                pos.set(anchorX - w / 2f, anchorY - h / 2f);
                rot = Maths.dirToRot(Math.toRadians(directionX), Math.toRadians(directionY));
                speed = Math.sqrt(directionX * directionX + directionY * directionY) * 0.1f;
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseMove(int x, int y) {
        if (super.mouseMove(x, y))
            return true;

        if (mousePressed) {
            directionX = x - anchorX;
            directionY = y - anchorY;
            return true;
        }

        return false;
    }
}
