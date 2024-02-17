package mayo.gui.widgets.types;

import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Resource;
import mayo.utils.UIHelper;

public class Scrollbar extends Slider {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/scrollbar.png"));

    public Scrollbar(int x, int y, int size) {
        super(x, y, size, 8);
        this.setVertical(true);
        this.showValueTooltip(false);
    }

    @Override
    protected void renderHorizontal(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderScrollbar(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderVertical(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderScrollbar(matrices, mouseX, mouseY, delta);
    }

    protected void renderScrollbar(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int id = TEXTURE.getID();

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int s = 8 * getState();

        //background
        UIHelper.nineQuad(VertexConsumer.GUI, matrices, id,
                x, y, w, h,
                s, 0f,
                8, 8,
                24, 16
        );

        float anim = getAnimationValue();
        if (isVertical())
            y += Math.round((h - 8f) * anim);
        else
            x += Math.round((w - 8f) * anim);

        //button
        VertexConsumer.GUI.consume(GeometryHelper.quad(
                matrices, x, y, 8, 8,
                s, 8f,
                8, 8,
                24, 16
        ), id);
    }
}
