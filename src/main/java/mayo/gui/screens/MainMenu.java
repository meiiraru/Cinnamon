package mayo.gui.screens;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.widgets.ContainerGrid;
import mayo.gui.widgets.Tickable;
import mayo.gui.widgets.Widget;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.*;
import mayo.world.WorldClient;

import java.util.function.Consumer;

public class MainMenu extends Screen {

    private Star[] stars;

    @Override
    public void init() {
        super.init();

        //may~o
        Text may = Text.of("May~o v%s \u25E0\u25DE\u25DF\u25E0".formatted(Client.VERSION)).withStyle(Style.EMPTY.italic(true).color(0x66FFFFFF).shadow(true).shadowColor(0x66161616));
        this.addWidget(new Label(width - TextUtils.getWidth(may, font) - 4, height - TextUtils.getHeight(may, font) - 4, may, font));

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

        //stars
        stars = new Star[(int) Math.ceil(height / 24f)];
        for (int i = 0; i < stars.length; i++)
            stars[i] = genStar(true);
    }

    @Override
    public void tick() {
        super.tick();

        for (int i = 0; i < stars.length; i++) {
            Star star = stars[i];
            star.tick();
            if (star.getTrueX() + star.getWidth() < 0)
                stars[i] = genStar(false);
        }
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        GeometryHelper.rectangle(VertexConsumer.GUI, matrices, 0, 0, width, height, -999, 0xFF29224B, 0xFF2E2557, 0xFF553C89, 0xFFDBA8DC);
        for (Star star : stars)
            star.render(matrices, 0, 0, delta);
    }

    private Star genStar(boolean init) {
        return new Star(
                init ? (int) (Math.random() * (width + 32) - 16) : width,
                (int) (Math.random() * (height + 16) - 8),
                (int) (Math.random() * 8 + 8),
                (int) (Math.random() * 4 + 1),
                (int) (Math.random() * 9 + 1)
        );
    }

    private static class MainButton extends Button {

        protected static final Texture LINE = Texture.of(new Resource("textures/gui/widgets/main_menu/line.png"));

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

            matrices.push();
            matrices.translate(0, 0, -1f);

            VertexConsumer.GUI.consume(GeometryHelper.quad(
                    matrices, getCenterX() - 64, getY(),
                    128, 32
            ), LINE.getID());

            matrices.pop();
        }
    }

    private static class Star extends Widget implements Tickable {
        private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/main_menu/star.png"));

        private final Animation2D translate, rotate;

        public Star(int x, int y, int size, float translate, float rotate) {
            super(x, y, size, size);
            this.translate = new Animation2D.Translate(-translate, 0);
            this.rotate = new Animation2D.Rotate(rotate);
        }

        @Override
        public void tick() {
            translate.tick();
            rotate.tick();
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.push();
            rotate.setAnchor(getX() + translate.getX(delta), getY() + translate.getY(delta));
            rotate.apply(matrices, delta);
            translate.apply(matrices, delta);

            VertexConsumer.GUI.consume(GeometryHelper.quad(
                    matrices,
                    getX(), getY(),
                    getWidth(), getHeight()
            ), TEXTURE.getID());

            matrices.pop();
        }

        public int getTrueX() {
            return getX() + (int) translate.getX(1f);
        }
    }
}
