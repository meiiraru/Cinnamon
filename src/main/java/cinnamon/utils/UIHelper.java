package cinnamon.utils;

import cinnamon.Client;
import cinnamon.gui.GUIStyle;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.gui.widgets.Widget;
import cinnamon.model.Vertex;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import org.joml.Matrix4f;

import java.util.Stack;

import static cinnamon.model.GeometryHelper.quad;
import static org.lwjgl.opengl.GL11.*;

public class UIHelper {

    public static final Resource TOOLTIP_TEXTURE = new Resource("textures/gui/widgets/tooltip.png");
    private static final Stack<Region2D> SCISSORS_STACK = new Stack<>();

    public static void renderBackground(MatrixStack matrices, int width, int height, float delta, Resource... background) {
        Client c = Client.getInstance();
        float speed = 0.125f;
        int textureSize = 64;
        width += textureSize;
        height += textureSize;

        for (int i = 0; i < background.length; i++) {
            Resource res = background[i];
            float x = 0, y = 0;
            float d = (c.ticks + delta) * speed * i;

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
            ), res);
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

    public static void nineQuad(VertexConsumer consumer, MatrixStack matrices, Resource texture, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        for (Vertex[] vertices : nineQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight))
            consumer.consume(vertices, texture);
    }

    public static void nineQuad(VertexConsumer consumer, MatrixStack matrices, Resource texture, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        for (Vertex[] vertices : nineQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)) {
            for (Vertex vertex : vertices)
                vertex.color(color);
            consumer.consume(vertices, texture);
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
            length1 = Math.round(Math.min(regionLength / 3f, length / 2f));
            length3 = Math.min(Math.max(length - length1, 0f), length1);
            length2 = Math.max(length - length1 - length3, 0f);

            pos2 = pos + length1;
            pos3 = pos2 + length2;

            centerRegion = regionLength - length1 - length3;
            uv2 = uv + length1;
            uv3 = uv + regionLength - length3;
        }
    }

    public static void horizontalQuad(VertexConsumer consumer, MatrixStack matrices, Resource texture, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        for (Vertex[] vertices : horizontalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight))
            consumer.consume(vertices, texture);
    }

    public static void horizontalQuad(VertexConsumer consumer, MatrixStack matrices, Resource texture, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        for (Vertex[] vertices : horizontalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)) {
            for (Vertex vertex : vertices)
                vertex.color(color);
            consumer.consume(vertices, texture);
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

    public static void verticalQuad(VertexConsumer consumer, MatrixStack matrices, Resource texture, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        for (Vertex[] vertices : verticalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight))
            consumer.consume(vertices, texture);
    }

    public static void verticalQuad(VertexConsumer consumer, MatrixStack matrices, Resource texture, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        for (Vertex[] vertices : verticalQuad(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)) {
            for (Vertex vertex : vertices)
                vertex.color(color);
            consumer.consume(vertices, texture);
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

    public static void renderTooltip(MatrixStack matrices, int x, int y, int width, int height, int centerX, int centerY, byte arrowSide, Text tooltip, Font font) {
        matrices.push();
        matrices.translate(x, y, 998f);

        //background
        int b = GUIStyle.tooltipBorder;
        UIHelper.nineQuad(VertexConsumer.GUI, matrices, TOOLTIP_TEXTURE, -b, -b, width + b + b, height + b + b, 0, 0, 16, 16, 20, 20, GUIStyle.accentColor);

        //arrow
        Vertex[] vertices = switch (arrowSide) {
            //right
            case 0 -> quad(matrices, -b - 4, -y + centerY - 8, 4, 16, 20, 0, -4, 16, 20, 20);
            //left
            case 1 -> quad(matrices, width + b, -y + centerY - 8, 4, 16, 16, 0, 4, 16, 20, 20);
            //up
            case 2 -> quad(matrices, -x + centerX - 8, height + b, 16, 4, 0, 16, 16, 4, 20, 20);
            //down
            default -> quad(matrices, -x + centerX - 8, -b - 4, 16, 4, 0, 20, 16, -4, 20, 20);
        };

        for (Vertex vertex : vertices)
            vertex.color(GUIStyle.accentColor);
        VertexConsumer.GUI.consume(vertices, TOOLTIP_TEXTURE);

        //render text
        font.render(VertexConsumer.FONT, matrices, 0, 0, tooltip);

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

    public static void pushScissors(int x, int y, int width, int height) {
        Window w = Client.getInstance().window;
        float guiScale = w.guiScale;

        int x2 = Math.round(x * guiScale);
        int y2 = w.height - Math.round(y * guiScale);
        int w2 = Math.round(width * guiScale);
        int h2 = Math.round(height * guiScale);

        Region2D region = new Region2D(x2, y2 - h2, x2 + w2, y2);

        if (!SCISSORS_STACK.isEmpty()) {
            Region2D peek = SCISSORS_STACK.peek();
            region.clip(peek);
        }

        SCISSORS_STACK.push(region);

        VertexConsumer.finishAllBatches(Client.getInstance().camera.getOrthographicMatrix(), new Matrix4f());

        glEnable(GL_SCISSOR_TEST);
        glScissor(region.getX(), region.getY(), region.getWidth(), region.getHeight());
    }

    public static void popScissors() {
        SCISSORS_STACK.pop();

        VertexConsumer.finishAllBatches(Client.getInstance().camera.getOrthographicMatrix(), new Matrix4f());

        if (!SCISSORS_STACK.isEmpty()) {
            Region2D peek = SCISSORS_STACK.peek();
            glEnable(GL_SCISSOR_TEST);
            glScissor(peek.getX(), peek.getY(), peek.getWidth(), peek.getHeight());
        } else {
            glDisable(GL_SCISSOR_TEST);
        }
    }
}
