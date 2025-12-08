package cinnamon.utils;

import cinnamon.Client;
import cinnamon.gui.GUIStyle;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.AlignedWidget;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.gui.widgets.Widget;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import org.joml.Math;

import java.util.Stack;

import static cinnamon.model.GeometryHelper.quad;
import static org.lwjgl.opengl.GL11.*;

public class UIHelper {

    private static final float DEPTH_OFFSET = 0.01f;
    private static final Stack<Vertex[]> STENCIL_STACK = new Stack<>();

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

            VertexConsumer.MAIN.consume(quad(
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
        Widget parent = w.getParent();
        if (parent != null && !(parent instanceof PopupWidget) && !isMouseOver(parent, mouseX, mouseY))
            return false;

        int x, y;
        if (w instanceof AlignedWidget aw) {
            x = aw.getAlignedX();
            y = aw.getAlignedY();
        } else {
            x = w.getX();
            y = w.getY();
        }

        return isMouseOver(x, y, w.getWidth(), w.getHeight(), mouseX, mouseY);
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

    public static void renderTooltip(MatrixStack matrices, int x, int y, Text tooltip) {
        int xx = x + 12;
        int yy = y + 12;
        int w = TextUtils.getWidth(tooltip);
        int h = TextUtils.getHeight(tooltip);
        Window win = Client.getInstance().window;

        if (xx + w > win.getGUIWidth())
            xx = x - 12 - w;
        if (yy + h > win.getGUIHeight())
            yy = y - 12 - h;

        renderTooltip(matrices, xx, yy, w, h, xx, yy, (byte) -1, tooltip, tooltip.getStyle().getGuiStyle());
    }

    public static void renderTooltip(MatrixStack matrices, int x, int y, int width, int height, int centerX, int centerY, byte arrowSide, Text tooltip, GUIStyle style) {
        matrices.pushMatrix();
        matrices.translate(x, y, 0);

        //background
        int b = style.getInt("tooltip_border");
        int color = style.getInt("accent_color");
        Resource tex = style.getResource("tooltip_tex");
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, tex, -b, -b, width + b + b, height + b + b, 0, 0, 16, 16, 20, 20, color);

        //arrow
        Vertex[] vertices = switch (arrowSide) {
            //right
            case 0 -> quad(matrices, -b - 4, -y + centerY - 8, 4, 16, 20, 0, -4, 16, 20, 20);
            //left
            case 1 -> quad(matrices, width + b, -y + centerY - 8, 4, 16, 16, 0, 4, 16, 20, 20);
            //up
            case 2 -> quad(matrices, -x + centerX - 8, height + b, 16, 4, 0, 16, 16, 4, 20, 20);
            //down
            case 3 -> quad(matrices, -x + centerX - 8, -b - 4, 16, 4, 0, 20, 16, -4, 20, 20);
            default -> null;
        };

        if (vertices != null) {
            for (Vertex vertex : vertices)
                vertex.color(color);
            VertexConsumer.MAIN.consume(vertices, tex);
        }

        //render text
        matrices.pushMatrix();
        matrices.translate(0, 0, getDepthOffset());
        tooltip.render(VertexConsumer.MAIN, matrices, 0, 0);

        matrices.popMatrix();
        matrices.popMatrix();
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

        Window window = Client.getInstance().window;

        popup.setPos(x, y);
        popup.fitToScreen(window.getGUIWidth(), window.getGUIHeight());

        fitInsideBoundaries(popup, 0, 0, window.getGUIWidth(), window.getGUIHeight());

        if (sPopup != popup) {
            s.popup = popup;
            s.addWidget(popup);
        }
    }

    public static PopupWidget getScreenPopup() {
        Screen s = Client.getInstance().screen;
        if (s == null)
            return null;
        return s.popup;
    }

    public static void focusWidget(SelectableWidget w) {
        Screen s = Client.getInstance().screen;
        if (s != null) s.focusWidget(w);
    }

    public static SelectableWidget getFocusedWidget() {
        Screen s = Client.getInstance().screen;
        return s != null ? s.getFocusedWidget() : null;
    }

    public static void xrWidgetHovered(SelectableWidget w) {
        Screen s = Client.getInstance().screen;
        if (s != null) s.xrWidgetHovered(w);
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

        if (toMove instanceof PopupWidget popup)
            popup.fitToScreen(window.getGUIWidth(), window.getGUIHeight());

        fitInsideBoundaries(toMove, 0, 0, window.getGUIWidth(), window.getGUIHeight());

        if (toMove.getX() < x) {
            toMove.setX(source.getX() - toMove.getWidth() - hOffset);
            fitInsideBoundaries(toMove, 0, 0, window.getGUIWidth(), window.getGUIHeight());
        }
    }

    public static void fitInsideBoundaries(Widget w, int x0, int y0, int x1, int y1) {
        //fix widget pos
        int x, y;
        int ww = w.getWidth();
        int wh = w.getHeight();

        if (w instanceof AlignedWidget aw) {
            int wwo = (int) aw.getAlignment().getWidthOffset(ww);
            int who = (int) aw.getAlignment().getHeightOffset(wh);
            int maxx = ww + wwo;
            int maxy = wh + who;
            x = Maths.clamp(w.getX(), x0 - wwo, x1 - maxx);
            y = Maths.clamp(w.getY(), y0 - who, y1 - maxy);
        } else {
            x = Maths.clamp(w.getX(), x0, x1 - ww);
            y = Maths.clamp(w.getY(), y0, y1 - wh);
        }

        w.setPos(x, y);
    }

    public static float tickDelta(float speed) {
        return 1f - Maths.pow(speed, Client.getInstance().timer.tickDelta);
    }

    public static void pushStencil(MatrixStack matrices, int x, int y, int width, int height) {
        Vertex[] vertices = quad(matrices, x, y, width, height);
        STENCIL_STACK.push(vertices);
        pushStencil(vertices);
    }

    private static void pushStencil(Vertex[] vertices) {
        prepareStencil(false, true);
        glDisable(GL_DEPTH_TEST);

        VertexConsumer.MAIN.consume(vertices);
        VertexConsumer.MAIN.finishBatch(Client.getInstance().camera);

        glEnable(GL_DEPTH_TEST);
        lockStencil(false);
    }

    public static void popStencil() {
        STENCIL_STACK.pop();
        if (STENCIL_STACK.isEmpty())
            disableStencil();
        else
            pushStencil(STENCIL_STACK.peek());
    }

    public static void prepareStencil(boolean allowDrawing, boolean clear) {
        VertexConsumer.finishAllBatches(Client.getInstance().camera);

        if (!allowDrawing) {
            glColorMask(false, false, false, false);
            glDepthMask(false);
        }
        glStencilMask(0xFF);

        glEnable(GL_STENCIL_TEST);
        if (clear) glClear(GL_STENCIL_BUFFER_BIT);
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        glStencilFunc(GL_ALWAYS, 1, 0xFF);
    }

    public static void lockStencil(boolean inverted) {
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glStencilMask(0x00);

        glStencilFunc(inverted ? GL_NOTEQUAL : GL_EQUAL, 1, 0xFF);
    }

    public static void disableStencil() {
        VertexConsumer.finishAllBatches(Client.getInstance().camera);
        glDisable(GL_STENCIL_TEST);
    }

    public static float getDepthOffset() {
        return XrManager.isInXR() ? XrRenderer.DEPTH_OFFSET : DEPTH_OFFSET;
    }
}
