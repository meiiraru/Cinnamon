package mayo.utils;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.widgets.PopupWidget;
import mayo.gui.widgets.SelectableWidget;
import mayo.gui.widgets.Widget;
import mayo.model.Vertex;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;

import static mayo.model.GeometryHelper.quad;

public class UIHelper {

    private static final Texture TOOLTIP = Texture.of(new Resource("textures/gui/widgets/tooltip.png"));
    public static final Colors ACCENT = Colors.PURPLE;

    public static void renderBackground(MatrixStack matrices, int width, int height, float delta, Texture... background) {
        Client c = Client.getInstance();
        float speed = 0.125f;
        int textureSize = 64;
        width += textureSize;
        height += textureSize;

        for (Texture texture : background) {
            float x = 0, y = 0;
            float d = (c.ticks + delta) * speed;

            x -= d % textureSize;
            y -= d % textureSize;

            float u1 = (float) width / textureSize;
            float v1 = (float) height / textureSize;

            VertexConsumer.GUI.consume(quad(
                    matrices,
                    x, y,
                    width, height,
                    -999,
                    0f, u1,
                    0f, v1
            ), texture.getID());

            speed *= 2f;
        }
    }

    public static boolean isWidgetHovered(Widget widget) {
        Window w = Client.getInstance().window;
        return isMouseOver(widget, w.mouseX, w.mouseY);
    }

    public static boolean isMouseOver(Widget w, int mouseX, int mouseY) {
        return isMouseOver(w.getX(), w.getY(), w.getWidth(), w.getHeight(), mouseX, mouseY);
    }

    public static boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void nineQuad(VertexConsumer consumer, MatrixStack matrices, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        for (Vertex[] vertices : nineQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight))
            consumer.consume(vertices, textureID);
    }

    public static void nineQuad(VertexConsumer consumer, MatrixStack matrices, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        for (Vertex[] vertices : nineQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)) {
            for (Vertex vertex : vertices)
                vertex.color(color);
            consumer.consume(vertices, textureID);
        }
    }

    private static Vertex[][] nineQuad(MatrixStack matrices, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        SplitQuad w = new SplitQuad(width, regionWidth, x, u);
        SplitQuad h = new SplitQuad(height, regionHeight, y, v);

        //draw
        Vertex[][] vertices = new Vertex[9][4];

        //top left
        vertices[0] = quad(matrices, x, y, w.length1, h.length1, u, v, w.length1, h.length1, textureWidth, textureHeight);
        //top middle
        vertices[1] = quad(matrices, w.pos2, y, w.length2, h.length1, w.uv2, v, w.centerRegion, h.length1, textureWidth, textureHeight);
        //top right
        vertices[2] = quad(matrices, w.pos3, y, w.length3, h.length1, w.uv3, v, w.length3, h.length1, textureWidth, textureHeight);

        //middle left
        vertices[3] = quad(matrices, x, h.pos2, w.length1, h.length2, u, h.uv2, w.length1, h.centerRegion, textureWidth, textureHeight);
        //middle middle
        vertices[4] = quad(matrices, w.pos2, h.pos2, w.length2, h.length2, w.uv2, h.uv2, w.centerRegion, h.centerRegion, textureWidth, textureHeight);
        //middle right
        vertices[5] = quad(matrices, w.pos3, h.pos2, w.length3, h.length2, w.uv3, h.uv2, w.length3, h.centerRegion, textureWidth, textureHeight);

        //bottom left
        vertices[6] = quad(matrices, x, h.pos3, w.length1, h.length3, u, h.uv3, w.length1, h.length3, textureWidth, textureHeight);
        //bottom middle
        vertices[7] = quad(matrices, w.pos2, h.pos3, w.length2, h.length3, w.uv2, h.uv3, w.centerRegion, h.length3, textureWidth, textureHeight);
        //bottom right
        vertices[8] = quad(matrices, w.pos3, h.pos3, w.length3, h.length3, w.uv3, h.uv3, w.length3, h.length3, textureWidth, textureHeight);

        return vertices;
    }

    private static class SplitQuad {
        private final float
                length1, length2, length3,
                pos2, pos3,
                centerRegion,
                uv2, uv3;
        public SplitQuad(float length, float regionLength, float pos, float uv) {
            length1 = Math.round(Math.min(regionLength / 3f, Math.min(length / 2f, length)));
            length3 = Math.min(Math.max(length - length1, 0f), length1);
            length2 = Math.max(length - length1 - length3, 0f);

            pos2 = pos + length1;
            pos3 = pos2 + length2;

            centerRegion = regionLength - length1 - length3;
            uv2 = uv + length1;
            uv3 = uv + regionLength - length3;
        }
    }

    public static void horizontalQuad(VertexConsumer consumer, MatrixStack matrices, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        for (Vertex[] vertices : horizontalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight))
            consumer.consume(vertices, textureID);
    }

    public static void horizontalQuad(VertexConsumer consumer, MatrixStack matrices, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        for (Vertex[] vertices : horizontalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)) {
            for (Vertex vertex : vertices)
                vertex.color(color);
            consumer.consume(vertices, textureID);
        }
    }

    private static Vertex[][] horizontalQuad(MatrixStack matrices, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        SplitQuad w = new SplitQuad(width, regionWidth, x, u);

        Vertex[][] vertices = new Vertex[3][4];

        vertices[0] = quad(matrices, x, y, w.length1, height, u, v, w.length1, regionHeight, textureWidth, textureHeight);
        vertices[1] = quad(matrices, w.pos2, y, w.length2, height, w.uv2, v, w.centerRegion, regionHeight, textureWidth, textureHeight);
        vertices[2] = quad(matrices, w.pos3, y, w.length3, height, w.uv3, v, w.length3, regionHeight, textureWidth, textureHeight);

        return vertices;
    }

    public static void verticalQuad(VertexConsumer consumer, MatrixStack matrices, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        for (Vertex[] vertices : verticalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight))
            consumer.consume(vertices, textureID);
    }

    public static void verticalQuad(VertexConsumer consumer, MatrixStack matrices, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        for (Vertex[] vertices : verticalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)) {
            for (Vertex vertex : vertices)
                vertex.color(color);
            consumer.consume(vertices, textureID);
        }
    }

    private static Vertex[][] verticalQuad(MatrixStack matrices, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        SplitQuad h = new SplitQuad(height, regionHeight, y, v);

        Vertex[][] vertices = new Vertex[3][4];

        vertices[0] = quad(matrices, x, y, width, h.length1, u, v, regionWidth, h.length1, textureWidth, textureHeight);
        vertices[1] = quad(matrices, x, h.pos2, width, h.length2, u, h.uv2, regionWidth, h.centerRegion, textureWidth, textureHeight);
        vertices[2] = quad(matrices, x, h.pos3, width, h.length3, u, h.uv3, regionWidth, h.length3, textureWidth, textureHeight);

        return vertices;
    }

    public static void renderTooltip(MatrixStack matrices, SelectableWidget widget, Font font) {
        //grab text
        Text tooltip = widget.getTooltip();
        if (tooltip == null || tooltip.isEmpty())
            return;

        //dimensions
        int w = TextUtils.getWidth(tooltip, font);
        int h = TextUtils.getHeight(tooltip, font);
        boolean lefty = false;

        Window window = Client.getInstance().window;
        int screenW = window.scaledWidth;
        int screenH = window.scaledHeight;

        int ww = widget.getWidth();

        //position
        int wcy = widget.getCenterY();

        int x = widget.getX() + ww + 1 + 2; //spacing + arrow
        int y = h > widget.getHeight() ? widget.getY() - 1 : widget.getCenterY() - h / 2 - 2;

        //check if the tooltip could be rendered on the left side
        if (x + w > screenW && widget.getCenterX() > screenW / 2) {
            x -= w + ww + 4 + 2 + 4; //(background + spacing + arrow) * 2
            lefty = true;
        }

        //then fit in the screen boundaries
        x = (int) Math.clamp(x, -2f, screenW - w - 2);
        y = (int) Math.clamp(y, -2f, screenH - h - 2);

        //render background

        //move matrices on x and z
        matrices.push();
        matrices.translate(x, 0, 998f);

        int texID = TOOLTIP.getID();

        //render arrow
        VertexConsumer.GUI.consume(quad(matrices, lefty ? w + 4f : -2, wcy - 8, 2, 16, lefty ? 18f : 16f, 0f, 2, 16, 20, 16), texID);

        //move matrices on y
        matrices.translate(0, y, 0);

        //render background
        nineQuad(VertexConsumer.GUI, matrices, texID, 0f, 0f, w + 4f, h + 4f, 0f, 0f, 16, 16, 20, 16);

        //render text
        font.render(VertexConsumer.FONT, matrices, 2, 2, tooltip);

        matrices.pop();
    }

    public static void setTooltip(SelectableWidget tooltip) {
        Screen s = Client.getInstance().screen;
        if (s != null) s.tooltip = tooltip;
    }

    public static void setPopup(int x, int y, PopupWidget popup) {
        Screen s = Client.getInstance().screen;
        if (s == null)
            return;

        PopupWidget sPopup = s.popup;
        if (sPopup != popup && sPopup != null) {
            s.removeWidget(sPopup);
            sPopup.close();
        }

        if (popup == null)
            return;

        popup.setPos(x, y);

        Window window = Client.getInstance().window;
        fitInsideBoundaries(popup, 0, 0, window.scaledWidth, window.scaledHeight);

        if (sPopup != popup) {
            s.popup = popup;
            s.addWidgetOnTop(popup);
        }
    }

    public static void focusWidget(SelectableWidget w) {
        Screen s = Client.getInstance().screen;
        if (s != null) s.focusWidget(w);
    }

    public static SelectableWidget getFocusedWidget() {
        Screen s = Client.getInstance().screen;
        return s != null ? s.getFocusedWidget() : null;
    }

    public static boolean isPopupHovered() {
        Screen s = Client.getInstance().screen;
        return s != null && s.popup != null && s.popup.isOpen() && s.popup.isHovered();
    }

    public static void moveWidgetRelativeTo(Widget source, Widget toMove, int hOffset) {
        //set the first pos
        int x = source.getX() + source.getWidth() + hOffset;
        toMove.setPos(x, source.getY());

        //fit to window
        Window window = Client.getInstance().window;
        fitInsideBoundaries(toMove, 0, 0, window.scaledWidth, window.scaledHeight);

        if (toMove.getX() < x) {
            toMove.setX(source.getX() - toMove.getWidth() - hOffset);
            fitInsideBoundaries(toMove, 0, 0, window.scaledWidth, window.scaledHeight);
        }
    }

    public static void fitInsideBoundaries(Widget w, int x0, int y0, int x1, int y1) {
        //fix widget pos
        int x = Math.clamp(w.getX(), x0, x1 - w.getWidth());
        int y = Math.clamp(w.getY(), y0, y1 - w.getHeight());
        w.setPos(x, y);
    }

    public static float tickDelta(float speed) {
        return (float) (1f - Math.pow(speed, Client.getInstance().timer.tickDelta));
    }
}
