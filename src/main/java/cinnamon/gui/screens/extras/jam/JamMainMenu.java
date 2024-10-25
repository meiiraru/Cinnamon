package cinnamon.gui.screens.extras.jam;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.Button;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;

public class JamMainMenu extends ParentedScreen {

    private static final Resource MENU = new Resource("textures/jam/main_menu.png");

    public JamMainMenu(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        if (client.window.scaledWidth != 500 || client.window.scaledHeight != 300) {
            int scale = Math.max(Math.round(Math.min(client.window.width / 500f, client.window.height / 300f)), 1);
            client.window.resize(500 * scale, 300 * scale);
            return;
        }

        Button play = new Button(151, 123, 194, 30, null, butt -> client.setScreen(new JamScreen(this)));
        play.setInvisible(true);
        addWidget(play);
        Button tutorial = new Button(151, 163, 194, 30, null, butt -> client.setScreen(new TutorialScreen(this)));
        tutorial.setInvisible(true);
        addWidget(tutorial);
        Button credits = new Button(151, 200, 194, 30, null, butt -> client.setScreen(new CreditsScreen(this)));
        credits.setInvisible(true);
        addWidget(credits);
        super.init();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, 0, width, height), MENU);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
