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
import mayo.utils.Meth;
import mayo.utils.Resource;
import org.joml.Vector2f;

public class DVDScreen extends Screen {

    private static final Texture DVD_TEX = new Texture(new Resource("textures/dvd.png"));
    private static final int w = 58, h = 40;
    private static final float speed = 2f;

    private final Screen parentScreen;
    private final Vector2f
            pos = new Vector2f(),
            dir = new Vector2f(speed, speed);
    private int color = -1;

    public DVDScreen(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();

        //go back
        this.addWidget(new Button(width - 60 - 4, height - 20 - 4, 60, 20 , Text.of("Back"), this::close));

        //fullscreen toast
        Toast.addToast(Text.of("Press [F11] to toggle fullscreen"), font);
    }

    @Override
    public void tick() {
        super.tick();

        pos.add(dir);

        if (pos.x + w >= width || pos.x <= 0f) {
            changeColor();
            dir.x *= -1;
            pos.x = Meth.clamp(pos.x, 0, width - w);
        }
        if (pos.y + h >= height || pos.y <= 0f) {
            changeColor();
            dir.y *= -1;
            pos.y = Meth.clamp(pos.y, 0, height - h);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Vertex[] vertices = GeometryHelper.quad(pos.x + dir.x * delta, pos.y + dir.y * delta, w, h);
        for (Vertex vertex : vertices)
            vertex.color(color);
        VertexConsumer.GUI.consume(vertices, DVD_TEX.getID());

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void changeColor() {
        color = (int) (Math.random() * 0x888888) + 0xFF777777;
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
