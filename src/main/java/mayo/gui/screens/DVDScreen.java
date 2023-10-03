package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.widgets.types.Button;
import mayo.model.GeometryHelper;
import mayo.model.Vertex;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.Meth;
import mayo.utils.Resource;
import org.joml.Vector2f;

public class DVDScreen extends Screen {

    private static final Texture DVD_TEX = Texture.of(new Resource("textures/gui/dvd.png"));
    private static final int w = 58, h = 40;
    private static final float speed = 2f;

    //normals
    private static final Vector2f
            UP = new Vector2f(0f, 1f),
            DOWN = new Vector2f(0f, -1f),
            LEFT = new Vector2f(-1f, 0f),
            RIGHT = new Vector2f(1f, 0f);

    private final Screen parentScreen;
    private final Vector2f
            pos = new Vector2f(),
            dir = new Vector2f();
    private Colors color;

    public DVDScreen(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();

        //go back
        this.addWidget(new Button(width - 60 - 4, height - 20 - 4, 60, 20 , Text.of("Back"), this::close));
    }

    @Override
    public void added() {
        //fullscreen toast
        Toast.addToast(Text.of("Press [F11] to toggle fullscreen"), font);

        //set color and position
        this.changeColor();
        pos.set((int) ((width - w) / 2f), (int) ((height - h) / 2f));
        dir.set(Math.random() < 0.5f ? speed : -speed, Math.random() < 0.5f ? speed : -speed);
    }

    @Override
    public void tick() {
        super.tick();

        pos.add(dir);

        //up
        if (pos.y <= 0f) {
            Meth.reflect(dir, UP);
            changeColor();
            pos.y = 0f;
        }
        //down
        if (pos.y + h >= height) {
            Meth.reflect(dir, DOWN);
            changeColor();
            pos.y = height - h;
        }
        //left
        if (pos.x <= 0f) {
            Meth.reflect(dir, LEFT);
            changeColor();
            pos.x = 0f;
        }
        //right
        if (pos.x + w >= width) {
            Meth.reflect(dir, RIGHT);
            changeColor();
            pos.x = width - w;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Vertex[] vertices = GeometryHelper.quad(matrices, pos.x + dir.x * delta, pos.y + dir.y * delta, w, h);
        for (Vertex vertex : vertices)
            vertex.color(color.rgba);
        VertexConsumer.GUI.consume(vertices, DVD_TEX.getID());

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void changeColor() {
        Colors temp = color;
        while (temp == color)
            temp = Colors.randomRainbow();
        this.color = temp;
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    public void close() {
        client.setScreen(parentScreen);
    }
}
