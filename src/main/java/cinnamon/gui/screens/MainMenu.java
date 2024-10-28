package cinnamon.gui.screens;

import cinnamon.Client;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.Tickable;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.texture.Texture;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.world.world.WorldClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MainMenu extends Screen {

    private static final Resource
        BACKGROUND = new Resource("textures/gui/main_menu/background.png"),
        STARS1 = new Resource("textures/gui/main_menu/stars1.png"),
        STARS2 = new Resource("textures/gui/main_menu/stars2.png"),
        OVERLAY = new Resource("textures/gui/main_menu/overlay.png"),
        TITLE_ROOT = new Resource("textures/gui/main_menu/title");
    private static final List<Resource> TITLE = new ArrayList<>();

    private Nebula[] nebula;

    @Override
    public void init() {
        super.init();

        //bottom texts
        Style s = Style.EMPTY.italic(true).color(0x66FFFFFF).shadow(true).shadowColor(0x66161616);
        Text bottomLeft = Text.of("Cinnamon v%s".formatted(Client.VERSION)).withStyle(s);
        this.addWidget(new Label(4, height - TextUtils.getHeight(bottomLeft, font) - 4, bottomLeft, font));

        Text bottomRight = Text.of("\u00A9").withStyle(s.italic(false)).append(Text.of("Meiiraru").withStyle(s));
        this.addWidget(new Label(width - TextUtils.getWidth(bottomRight, font) - 4, height - TextUtils.getHeight(bottomRight, font) - 4, bottomRight, font));

        //buttons
        ContainerGrid grid = new ContainerGrid(0, 0, 4);

        //open world
        Button worldButton = new MainButton(Text.of("Singleplayer").withStyle(Style.EMPTY.color(Colors.YELLOW)), button -> {
            //init client
            //if (ServerConnection.open()) {
                WorldClient world = new WorldClient();
                world.init();
            //} else {
            //    Toast.addToast(Text.of("Unable to create the internal server"), client.font);
            //}
        });
        grid.addWidget(worldButton);

        //multiplayer
        Button joinWorld = new MainButton(Text.of("Multiplayer"), button -> client.setScreen(new MultiplayerJoinScreen(this)));
        joinWorld.setTooltip(Text.of("Sorry, not available yet! ").append(Text.of("\u2764").withStyle(Style.EMPTY.color(Colors.PINK))));
        joinWorld.setActive(false);
        grid.addWidget(joinWorld);

        //extra stuff
        Button extras = new MainButton(Text.of("Extras"), button -> client.setScreen(new ExtrasScreen(this)));
        grid.addWidget(extras);

        //exit
        Button exitButton = new MainButton(Text.of("Exit"), button -> client.window.exit());
        exitButton.setTooltip(Text.of("bye~"));
        grid.addWidget(exitButton);

        //add grid to screen
        grid.setPos(12, (height - grid.getHeight()) / 2);
        this.addWidget(grid);

        //nebula
        nebula = new Nebula[(int) Math.ceil(height / 32f)];
        for (int i = 0; i < nebula.length; i++)
            nebula[i] = new Nebula((float) (Math.random() * width), (float) (Math.random() * height));
    }

    private void createNebula(int index) {
        int x, y;

        //vertical
        if (Math.random() < 0.5f) {
            x = (int) (Math.random() * width);
            y = Math.random() < 0.5f ? -Nebula.SIZE : height + Nebula.SIZE;
        }
        //horizontal
        else {
            x = Math.random() < 0.5f ? -Nebula.SIZE : width + Nebula.SIZE;
            y = (int) (Math.random() * height);
        }

        nebula[index] = new Nebula(x, y);
    }

    @Override
    public void tick() {
        for (int i = 0; i < nebula.length; i++) {
            Nebula n = nebula[i];
            n.tick();
            //create a new one when out of bounds
            if (n.x < -Nebula.SIZE || n.x > width || n.y < -Nebula.SIZE || n.y > height)
                createNebula(i);
        }
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        //background
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, 0, width, height, -999, 0f, 1f, 0f, 1f), BACKGROUND, true, false);

        //stars
        float time = (client.ticks + delta) * 0.02f;
        Vertex[] stars = GeometryHelper.quad(matrices, 0, 0, width, height, -999, 0f, width / 256f, 0f, height / 256f);
        for (Vertex vertex : stars)
            vertex.color(1f, 1f, 1f, (float) Math.sin(time) * 0.5f + 0.5f);
        VertexConsumer.GUI.consume(stars, STARS1, true, false);

        for (Vertex vertex : stars)
            vertex.color(1f, 1f, 1f, (float) Math.sin(-time) * 0.5f + 0.5f);
        VertexConsumer.GUI.consume(stars, STARS2, true, false);

        //nebula
        for (Nebula nebula : nebula)
            nebula.render(matrices, delta);

        //overlay
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, 0, width, height, -999, 0f, 1f, 0f, 1f), OVERLAY, true, false);

        //title
        renderTitle(matrices, delta);
    }

    private void renderTitle(MatrixStack matrices, float delta) {
        int width = 0;
        for (Resource title : TITLE)
            width += Texture.of(title).getWidth();

        float deltaTick = client.ticks + delta;
        float x = this.width * 0.66f - width * 0.5f;
        float y = this.height * 0.5f;

        for (int i = 0; i < TITLE.size(); i++) {
            Resource res = TITLE.get(i);
            Texture texture = Texture.of(res);
            int w = texture.getWidth();
            int h = texture.getHeight();

            float y2 = (float) Math.sin(deltaTick * 0.1f + i % 2) * 2f - h * 0.5f;
            VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, x, y + y2, w, h), res);
            x += w + 1;
        }
    }

    public static void initTextures() {
        List<String> titles = IOUtils.listResources(TITLE_ROOT);
        titles.sort(IOUtils.FilenameComparator::compareTo);

        TITLE.clear();
        for (String title : titles)
            TITLE.add(TITLE_ROOT.resolve(title));
    }

    private static class MainButton extends Button {

        protected static final Resource LINE = new Resource("textures/gui/widgets/main_menu/line.png");

        private float hoverX = -40f;

        public MainButton(Text message, Consumer<Button> action) {
            super(0, 0, 148, 20, message, action);
            message.withStyle(Style.EMPTY.shadow(true));
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.push();

            float d = UIHelper.tickDelta(0.6f);
            hoverX = Maths.lerp(hoverX, isHoveredOrFocused() ? 0 : -20, d);
            matrices.translate(hoverX, 0, 0);

            super.renderWidget(matrices, mouseX, mouseY, delta);

            matrices.pop();
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.isHoveredOrFocused())
                return;
            VertexConsumer.GUI.consume(GeometryHelper.quad(
                    matrices, getCenterX() - 64, getY(),
                    128, 32
            ), LINE);
        }
    }

    private static class Nebula implements Tickable {

        private static final Resource[] TEXTURES = {
                new Resource("textures/gui/main_menu/nebula1.png"),
                new Resource("textures/gui/main_menu/nebula2.png"),
                new Resource("textures/gui/main_menu/nebula3.png"),
        };
        private static final int SIZE = 256;
        private static final float ALPHA = 0.15f;

        private final Resource texture;
        private final float motionX, motionY;
        private final Colors color;

        private float x, y;

        public Nebula(float x, float y) {
            this.x = x;
            this.y = y;
            texture = TEXTURES[(int) (Math.random() * TEXTURES.length)];
            motionX = (float) (Math.random() * 2f - 1f) * 0.15f;
            motionY = (float) (Math.random() * 2f - 1f) * 0.15f;
            color = Colors.randomRainbow();
        }

        @Override
        public void tick() {
            x += motionX;
            y += motionY;
        }

        public void render(MatrixStack matrices, float delta) {
            float x = this.x + motionX * delta;
            float y = this.y + motionY * delta;
            Vertex[] vertices = GeometryHelper.quad(matrices, x, y, SIZE, SIZE, -999, 0f, 1f, 0f, 1f);
            for (Vertex vertex : vertices)
                vertex.color(color.r / 255f, color.g / 255f, color.b / 255f, ALPHA);
            VertexConsumer.GUI.consume(vertices, texture, true, false);
        }
    }
}
