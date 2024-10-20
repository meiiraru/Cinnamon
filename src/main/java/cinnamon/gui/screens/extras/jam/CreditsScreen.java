package cinnamon.gui.screens.extras.jam;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.Button;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;

public class CreditsScreen extends ParentedScreen {

    private static final Resource TEXTURE = new Resource("textures/jam/credits.png");

    public CreditsScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        Button back = new Button(366, 262, 124, 30, null, button -> close());
        back.setInvisible(true);
        addWidget(back);
        super.init();
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, 0, 0, width, height), TEXTURE);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
