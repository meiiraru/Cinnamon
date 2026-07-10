package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.AlignedWidget;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.HoverEvent;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;
import org.joml.Math;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Label extends SelectableWidget implements AlignedWidget {

    protected Text text;
    protected Text wrappedText;
    protected Alignment alignment;
    protected int maxWidth = -1;
    protected boolean renderBackground = true;
    protected boolean forceBackground = false;
    protected HoverEvent tooltipOverride;

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
        if (renderBackground() && (forceBackground() || (isSelectable() && isHoveredOrFocused())))
            renderHover(matrices, mouseX, mouseY, delta);

        renderText(matrices, mouseX, mouseY, delta);

        if (isHovered())
            renderTextHover(matrices, mouseX, mouseY, delta);
    }

    protected void renderHover(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, getSkin().getResource("label_tex"),
                getAlignedX() - 1, getAlignedY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                15, 15,
                15, 15
        );
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (wrappedText != null)
            wrappedText.render(VertexConsumer.MAIN, matrices, getX(), getY(), alignment);
    }

    protected void renderTextHover(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (wrappedText == null)
            return;

        tooltipOverride = null;

        int localX = mouseX - getAlignedX();
        int localY = mouseY - getAlignedY();
        Style s = TextUtils.getStyleAt(wrappedText, localX, localY, alignment);

        if (s != null && s.getHoverEvent() != null) {
            tooltipOverride = s.getHoverEvent();
            UIHelper.setTooltip(this);
        }
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
        if (this.text == null) {
            this.wrappedText = null;
            setDimensions(0, 0);
            return;
        }

        Text styledText = Text.empty().withStyle(Style.EMPTY.guiSkin(getSkinRes())).append(this.text);

        if (this.maxWidth > 0) {
            //split by newlines first, then wrap each line to maxWidth
            List<Text> wrappedLines = new ArrayList<>();
            for (Text line : TextUtils.split(styledText, "\n"))
                wrappedLines.addAll(TextUtils.warpToWidth(line, this.maxWidth));

            //join the lines back together with the newline
            this.wrappedText = TextUtils.join(wrappedLines, Text.of("\n"));
        } else {
            this.wrappedText = styledText;
        }

        setDimensions(TextUtils.getWidth(this.wrappedText), TextUtils.getHeight(this.wrappedText));
        super.updateDimensions();
    }

    protected void updateSelectable() {
        setSelectable(getTooltip() != null || getPopup() != null);
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (isActive() && isHoveredOrFocused() && button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_RELEASE) {
            int localX = Client.getInstance().window.mouseX - getAlignedX();
            int localY = Client.getInstance().window.mouseY - getAlignedY();
            Style s = TextUtils.getStyleAt(wrappedText, localX, localY, alignment);
            if (s != null && s.getClickEvent() != null) {
                s.getClickEvent().onClick();
                return this;
            }
        }

        return super.mousePress(button, action, mods);
    }

    @Override
    public void renderTooltip(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (tooltipOverride != null) {
            tooltipOverride.onHover(matrices, mouseX, mouseY, delta);
            tooltipOverride = null;
            return;
        }

        super.renderTooltip(matrices, mouseX, mouseY, delta);
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
    public void setSkin(Resource skin) {
        super.setSkin(skin);
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

    public void setMaxWidth(int maxWidth) {
        if (this.maxWidth != maxWidth) {
            this.maxWidth = maxWidth;
            updateDimensions();
        }
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public boolean renderBackground() {
        return renderBackground;
    }

    public void setRenderBackground(boolean renderBackground) {
        this.renderBackground = renderBackground;
    }

    public boolean forceBackground() {
        return forceBackground;
    }

    public void setForceBackground(boolean forceBackground) {
        this.forceBackground = forceBackground;
    }
}