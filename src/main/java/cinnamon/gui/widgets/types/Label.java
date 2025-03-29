package cinnamon.gui.widgets.types;

import cinnamon.gui.widgets.AlignedWidget;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

public class Label extends SelectableWidget implements AlignedWidget {

    private Text text;
    private Alignment alignment;

    public Label(int x, int y, Text text) {
        this(x, y, text, Alignment.TOP_LEFT);
    }

    public Label(int x, int y, Text text, Alignment alignment) {
        super(x, y, 0, 0);
        this.setText(text);
        this.setSelectable(false);
        this.setAlignment(alignment);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (isSelectable() && isHoveredOrFocused())
            renderHover(matrices, mouseX, mouseY, delta);
        renderText(matrices, mouseX, mouseY, delta);
    }

    protected void renderHover(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, getStyle().labelTex,
                getAlignedX() - 1, getAlignedY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                15, 15,
                15, 15
        );
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Text.empty().withStyle(Style.EMPTY.guiStyle(getStyleRes())).append(text).render(VertexConsumer.FONT, matrices, getX(), getY(), alignment);
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
        updateDimensions();
    }

    @Override
    protected void updateDimensions() {
        Text text = Text.empty().withStyle(Style.EMPTY.guiStyle(getStyleRes())).append(this.text);
        setDimensions(TextUtils.getWidth(text), TextUtils.getHeight(text));
        super.updateDimensions();
    }

    private void updateSelectable() {
        setSelectable(getTooltip() != null || getPopup() != null);
    }

    @Override
    public void setAlignment(Alignment alignment) {
        if (this.alignment == alignment)
            return;

        this.alignment = alignment;
        updateDimensions();
    }

    @Override
    public Alignment getAlignment() {
        return alignment;
    }

    @Override
    public int getAlignedX() {
        return getX() + Math.round(alignment.getWidthOffset(getWidth()));
    }

    @Override
    public int getAlignedY() {
        return getY() + Math.round(alignment.getHeightOffset(getHeight()));
    }

    @Override
    public void setStyle(Resource style) {
        super.setStyle(style);
        updateDimensions();
    }

    @Override
    public void setTooltip(Text tooltip) {
        super.setTooltip(tooltip);
        updateSelectable();
    }

    @Override
    public void setPopup(PopupWidget popup) {
        super.setPopup(popup);
        updateSelectable();
    }

    @Override
    public int getCenterX() {
        return getAlignedX() + getWidth() / 2;
    }

    @Override
    public int getCenterY() {
        return getAlignedY() + getHeight() / 2;
    }
}
