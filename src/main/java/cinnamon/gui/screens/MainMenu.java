package cinnamon.gui.screens;

import cinnamon.Client;
import cinnamon.gui.GUIStyle;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.model.GeometryHelper;
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
        OVERLAY = new Resource("textures/gui/main_menu/overlay.png"),
        BOTTOM = new Resource("textures/gui/main_menu/bottom.png"),
        TITLE_ROOT = new Resource("textures/gui/main_menu/title");
    private static final List<Resource> TITLE = new ArrayList<>();

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
        Button worldButton = new MainButton(Text.of("Singleplayer"), button -> {
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
        int y = (int) (height * 0.15f);
        grid.setPos((width - grid.getWidth()) / 2, y + (height - grid.getHeight() - y) / 2);
        this.addWidget(grid);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        //background
        Texture bg = Texture.of(BACKGROUND);
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, 0, width, height, -999, 0f, (float) width / bg.getWidth(), 0f, (float) height / bg.getHeight()), BACKGROUND, true, false);

        //bottom
        Texture bottom = Texture.of(BOTTOM);
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, height - bottom.getHeight(), width, bottom.getHeight(), -999, 0f, (float) width / bottom.getWidth(), 0f, 1f), BOTTOM, true, false);

        //overlay
        Texture overlay = Texture.of(OVERLAY);
        UIHelper.nineQuad(VertexConsumer.GUI, matrices, OVERLAY, 0, 0, width, height, 0f, 0f, overlay.getWidth(), overlay.getHeight(), overlay.getWidth(), overlay.getHeight());
        //VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, 0, width, height, -999, 0f, 1f, 0f, 1f), OVERLAY, true, false);

        //title
        renderTitle(matrices, delta);
    }

    private void renderTitle(MatrixStack matrices, float delta) {
        int width = 0;
        for (Resource title : TITLE)
            width += Texture.of(title).getWidth();

        float deltaTick = client.ticks + delta;
        float x = this.width * 0.5f - width * 0.5f;
        float y = this.height * 0.15f;

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

        protected static final Resource LINE = new Resource("textures/gui/main_menu/line.png");

        private float hoverY = 15f;

        public MainButton(Text message, Consumer<Button> action) {
            super(0, 0, 148, 20, message, action);
            message.withStyle(Style.EMPTY.outlined(true));
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.push();

            float d = UIHelper.tickDelta(0.6f);
            hoverY = Maths.lerp(hoverY, isHoveredOrFocused() ? -5 : 0, d);
            matrices.translate(0, hoverY, 0);

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

        @Override
        public Text getFormattedMessage() {
            Text text = super.getFormattedMessage();
            return !isHoveredOrFocused() || getState() == 0 ? text : Text.empty().append(text).withStyle(Style.EMPTY.color(GUIStyle.mainMenuTextColor));
        }
    }
}
