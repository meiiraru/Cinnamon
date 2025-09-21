package cinnamon.gui.screens;

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
import cinnamon.world.worldgen.GeneratedWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static cinnamon.render.texture.Texture.TextureParams.SMOOTH_SAMPLING;

public class MainMenu extends Screen {

    private static final Resource
        BACKGROUND = new Resource("textures/gui/main_menu/background.png"),
        OVERLAY    = new Resource("textures/gui/main_menu/overlay.png"),
        BOTTOM     = new Resource("textures/gui/main_menu/bottom.png"),
        TITLE_ROOT = new Resource("textures/gui/main_menu/title"),
        GUI_STYLE  = new Resource("data/gui_styles/main_menu.json");
    private static final List<Resource> TITLE = new ArrayList<>();

    @Override
    public void init() {
        super.init();

        //bottom texts
        Style s = Style.EMPTY.italic(true).color(0x66FFFFFF).shadow(true).shadowColor(0x66161616).guiStyle(GUI_STYLE);
        Text bottomLeft = Text.of("Cinnamon v%s".formatted(Version.CLIENT_VERSION.toStringNoBuild())).withStyle(s);
        this.addWidget(new Label(4, height - 4, bottomLeft, Alignment.BOTTOM_LEFT));

        Text bottomRight = Text.of("\u00A9").withStyle(s.italic(false)).append(Text.of("Meiiraru").withStyle(s));
        this.addWidget(new Label(width - 4, height - 4, bottomRight, Alignment.BOTTOM_RIGHT));

        //buttons
        ContainerGrid grid = new ContainerGrid(0, 0, 4);

        //open world
    Button worldButton = new MainButton(Text.translated("gui.main_menu.singleplayer"), button -> {
            //init client
            //if (ServerConnection.open()) {
        long seed = System.currentTimeMillis();
        WorldClient world = new GeneratedWorld("world", seed);
                world.init();
            //} else {
            //    Toast.addToast(Text.of("Unable to create the internal server"), client.font);
            //}
        });
        grid.addWidget(worldButton);

        //multiplayer
        Button joinWorld = new MainButton(Text.translated("gui.main_menu.multiplayer"), button -> client.setScreen(new MultiplayerJoinScreen(this)));
        joinWorld.setTooltip(Text.translated("gui.main_menu.multiplayer.not_available").append(Text.of(" \u2764").withStyle(Style.EMPTY.color(Colors.PINK))));
        joinWorld.setActive(false);
        grid.addWidget(joinWorld);

        //extra stuff
        Button extras = new MainButton(Text.translated("gui.main_menu.extras"), button -> client.setScreen(new ExtrasScreen(this)));
        grid.addWidget(extras);

        //exit
        Button exitButton = new MainButton(Text.translated("gui.exit"), button -> client.window.exit());
        exitButton.setTooltip(Text.translated("gui.main_menu.exit.tooltip"));
        grid.addWidget(exitButton);

        //add grid to screen
        int y = (int) (height * 0.15f);
        grid.setPos((width - grid.getWidth()) / 2, y + (height - grid.getHeight() - y) / 2);
        grid.setStyle(GUI_STYLE);
        this.addWidget(grid);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        renderSolidBackground(0xFFD3AB7A);

        float d = UIHelper.getDepthOffset();

        //background
        Texture bg = Texture.of(BACKGROUND);
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, 0, 0, width, height, -d * 3, 0f, (float) width / bg.getWidth(), 0f, (float) height / bg.getHeight()), BACKGROUND, SMOOTH_SAMPLING);

        //bottom
        Texture bottom = Texture.of(BOTTOM);
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, 0, height - bottom.getHeight(), width, bottom.getHeight(), -d * 2, 0f, (float) width / bottom.getWidth(), 0f, 1f), BOTTOM, SMOOTH_SAMPLING);

        //overlay
        Texture overlay = Texture.of(OVERLAY);
        matrices.pushMatrix();
        matrices.translate(0, 0, -d);
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, OVERLAY, 0, 0, width, height, 0f, 0f, overlay.getWidth(), overlay.getHeight(), overlay.getWidth(), overlay.getHeight());
        matrices.popMatrix();
        //VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, 0, width, height, -d, 0f, 1f, 0f, 1f), OVERLAY, true, false);

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
            VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x, y + y2, w, h), res);
            x += w + 1;
        }
    }

    public static void initTextures() {
        TITLE.clear();

        List<String> titles = IOUtils.listResources(TITLE_ROOT, false);
        if (titles == null)
            return;

        //1 in 10000 chance
        boolean funny = Math.random() < 0.0001f; 

        titles.sort(IOUtils.FilenameComparator::compareTo);
        for (String title : titles) {
            Resource res = TITLE_ROOT.resolve(title);
            TITLE.add(res);
            if (funny) TITLE.add(res);
        }
    }

    private static class MainButton extends Button {

        protected static final Resource LINE = new Resource("textures/gui/main_menu/line.png");

        private float hoverY = 15f;

        public MainButton(Text message, Consumer<Button> action) {
            super(0, 0, 148, 20, message, action);
            message.withStyle(Style.EMPTY.outlined(true));
            setStyle(GUI_STYLE);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.pushMatrix();

            float d = UIHelper.tickDelta(0.6f);
            hoverY = Maths.lerp(hoverY, isHoveredOrFocused() ? -5 : 0, d);
            matrices.translate(0, hoverY, 0);

            super.renderWidget(matrices, mouseX, mouseY, delta);

            matrices.popMatrix();
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.isHoveredOrFocused())
                return;
            VertexConsumer.MAIN.consume(GeometryHelper.quad(
                    matrices, getCenterX() - 64, getY(),
                    128, 32
            ), LINE);
        }
    }
}
