package cinnamon.gui.widgets.types;

import cinnamon.gui.widgets.AlignedWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

public class Label extends SelectableWidget implements AlignedWidget {

    private static final Resource HOVER_TEXTURE = new Resource("textures/gui/widgets/label.png");

    private final Font font;
    private Text text;
    private Alignment alignment = Alignment.LEFT;

    public Label(int x, int y, Text text, Font font) {
        super(x, y, TextUtils.getWidth(text, font), TextUtils.getHeight(text, font));
        this.text = text;
        this.font = font;
        this.setSelectable(false);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (isSelectable() && isHoveredOrFocused())
            renderHover(matrices, mouseX, mouseY, delta);
        renderText(matrices, mouseX, mouseY, delta);
    }

    protected void renderHover(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, HOVER_TEXTURE,
                getAlignedX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                15, 15,
                15, 15
        );
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        font.render(VertexConsumer.FONT, matrices, getX(), getY(), text, alignment);
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
        setWidth(TextUtils.getWidth(text, font));
        setHeight(TextUtils.getHeight(text, font));
    }

    @Override
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    @Override
    public int getAlignedX() {
        return getX() + Math.round(alignment.getOffset(getWidth()));
    }
}
