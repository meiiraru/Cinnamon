package cinnamon.gui.screens;

import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.math.Maths;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.texture.Texture;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.vr.XrManager;
import cinnamon.world.world.WorldClient;
import org.joml.Math;

import java.util.function.Consumer;

import static cinnamon.render.texture.Texture.TextureParams.SMOOTH_SAMPLING;

public class MainMenu extends Screen {

    public static final Resource
        BACKGROUND1 = new Resource("textures/gui/main_menu/background1.png"),
        BACKGROUND2 = new Resource("textures/gui/main_menu/background2.png"),
        OVERLAY     = new Resource("textures/gui/main_menu/overlay.png"),
        BOTTOM      = new Resource("textures/gui/main_menu/bottom.png"),
        TITLE       = new Resource("textures/logo.png"),
        GUI_SKIN    = new Resource("data/gui_skins/main_menu.json");

    @Override
    public void init() {
        super.init();

        //buttons
        ContainerGrid grid = new ContainerGrid(0, 0, 4);

        //open world
        Button worldButton = new MainButton(Text.translated("gui.main_menu.playground"), button -> {
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
//        Button joinWorld = new MainButton(Text.translated("gui.main_menu.multiplayer"), button -> client.setScreen(new MultiplayerJoinScreen(this)));
//        joinWorld.setTooltip(Text.translated("gui.main_menu.multiplayer.not_available").append(Text.of(" \u2764").withStyle(Style.EMPTY.color(Colors.PINK))));
//        joinWorld.setActive(false);
//        grid.addWidget(joinWorld);

        //settings
        Button settings = new MainButton(Text.translated("gui.main_menu.settings"), button -> client.setScreen(new SettingsScreen(this)));
        settings.setActive(false);
        grid.addWidget(settings);

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
        grid.setSkin(GUI_SKIN);
        this.addWidget(grid);

        //bottom texts
        Style s = Style.EMPTY.italic(true).color(0x66FFFFFF).shadow(true).shadowColor(0x66161616).guiSkin(GUI_SKIN);
        Text bottomLeft = Text.of("Cinnamon v%s".formatted(Version.CLIENT_VERSION.toStringNoBuild())).withStyle(s);
        this.addWidget(new Label(4, height - 4, bottomLeft, Alignment.BOTTOM_LEFT));

        Text bottomRight = Text.of("\u00A9").withStyle(s.italic(false)).append(Text.of("Kingdom of Moon").withStyle(s));
        int bottomRightWidth = TextUtils.getWidth(bottomRight);
        int bottomRightHeight = TextUtils.getHeight(bottomRight);
        Button credits = new Button(width - 4 - bottomRightWidth, height - 4 - bottomRightHeight, bottomRightWidth, bottomRightHeight, bottomRight, button -> client.setScreen(new CreditsScreen(this))) {
            @Override
            protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {}
        };
        credits.setTooltip(Text.translated("gui.main_menu.credits.tooltip"));
        this.addWidget(credits);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        renderSolidBackground(0xFFD3AB7A);

        boolean xr = XrManager.isInXR();
        float d = UIHelper.getDepthOffset();

        float parallaxX = Maths.clamp((float) client.window.mouseX / client.window.getGUIWidth()  * 2f - 1f, -1f, 1f);
        float parallaxY = Maths.clamp((float) client.window.mouseY / client.window.getGUIHeight() * 2f - 1f, -1f, 1f);

        //background
        Texture bg = Texture.of(BACKGROUND1);
        float bgParallax = xr ? 0f : 10f;
        float bgOffset = (client.ticks + delta) * 0.002f;
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices,
                -bgParallax + bgParallax * parallaxX, -bgParallax + bgParallax * parallaxY,
                width + bgParallax * 2f, height + bgParallax * 2f, -d * 3f,
                bgOffset, (float) width / bg.getWidth() + bgOffset, bgOffset, (float) height / bg.getHeight() + bgOffset
        ), BACKGROUND1, SMOOTH_SAMPLING);

        Texture bg2 = Texture.of(BACKGROUND2);
        float bg2Parallax = xr ? 0f : 20f;
        float bg2Offset = (client.ticks + delta) * 0.004f;
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices,
                -bg2Parallax + bg2Parallax * parallaxX, -bg2Parallax + bg2Parallax * parallaxY,
                width + bg2Parallax * 2f, height + bg2Parallax * 2f, -d * 2f,
                bg2Offset, (float) width / bg2.getWidth() + bg2Offset, bg2Offset, (float) height / bg2.getHeight() + bg2Offset
        ), BACKGROUND2, SMOOTH_SAMPLING);

        //bottom
        Texture bottom = Texture.of(BOTTOM);
        float bottomParallax = xr ? 0f : -bottom.getHeight() / 3f;
        float uOffset = (client.ticks + delta) * 0.015f;
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices,
                0f, -bottomParallax + bottomParallax * parallaxY + height - bottom.getHeight(),
                width, bottom.getHeight(), -d * 2f,
                uOffset, (float) width / bottom.getWidth() + uOffset, 0f, 1f
        ), BOTTOM, SMOOTH_SAMPLING);

        //overlay
        Texture overlay = Texture.of(OVERLAY);
        matrices.pushMatrix();
        matrices.translate(0, 0, -d);
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, OVERLAY, 0, 0, width, height, 0f, 0f, overlay.getWidth(), overlay.getHeight(), overlay.getWidth(), overlay.getHeight());
        matrices.popMatrix();

        //title
        Texture title = Texture.of(TITLE);

        float titleParallax = xr ? 0f : 3f;
        float x = this.width * 0.5f - title.getWidth() * 0.5f + parallaxX * titleParallax;
        float y = this.height * 0.25f - title.getHeight() * 0.5f + parallaxY * titleParallax;
        float y2 = Math.sin((client.ticks + delta) * 0.1f) * 2f;

        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x, y + y2, title.getWidth(), title.getHeight()), TITLE);
    }

    public static class MainButton extends Button {

        protected static final Resource LINE = new Resource("textures/gui/main_menu/line.png");

        private float hoverY = 15f;

        public MainButton(Text message, Consumer<Button> action) {
            super(0, 0, 148, 20, message, action);
            message.withStyle(Style.EMPTY.outlined(true));
            setSkin(GUI_SKIN);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.pushMatrix();

            float d = UIHelper.tickDelta(0.6f);
            hoverY = Math.lerp(hoverY, isHoveredOrFocused() ? -5 : 0, d);
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
