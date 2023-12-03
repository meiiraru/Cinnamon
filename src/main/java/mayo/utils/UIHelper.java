package mayo.utils;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.widgets.Widget;
import mayo.gui.widgets.types.ContextMenu;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;

import static mayo.model.GeometryHelper.quad;

public class UIHelper {

    private static final Texture[] BACKGROUND = new Texture[]{
            Texture.of(new Resource("textures/gui/background/background_0.png")),
            Texture.of(new Resource("textures/gui/background/background_1.png")),
            Texture.of(new Resource("textures/gui/background/background_2.png"))
    };
    private static final Texture TOOLTIP = Texture.of(new Resource("textures/gui/widgets/tooltip.png"));
    public static final Colors ACCENT = Colors.PURPLE;

    public static void renderBackground(MatrixStack matrices, int width, int height, float delta) {
        Client c = Client.getInstance();
        float speed = 0.125f;
        int textureSize = 64;
        width += textureSize;
        height += textureSize;

        for (Texture texture : BACKGROUND) {
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

    public static boolean isMouseOver(Widget w, int mouseX, int mouseY) {
        return isMouseOver(w.getX(), w.getY(), w.getWidth(), w.getHeight(), mouseX, mouseY);
    }

    public static boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void nineQuad(VertexConsumer vertexConsumer, MatrixStack matrices, int textureID, float x, float y, float width, float height) {
        nineQuad(vertexConsumer, matrices, textureID, x, y, width, height, 0f, 0f, 15, 15, 15, 15);
    }

    public static void nineQuad(VertexConsumer consumer, MatrixStack matrices, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        float rWidthThird = regionWidth / 3f;
        float rHeightThird = regionHeight / 3f;

        //top left
        consumer.consume(quad(matrices, x, y, rWidthThird, rHeightThird, u, v, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //top middle
        consumer.consume(quad(matrices, x + rWidthThird, y, width - rWidthThird * 2, rHeightThird, u + rWidthThird, v, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //top right
        consumer.consume(quad(matrices, x + width - rWidthThird, y, rWidthThird, rHeightThird, u + rWidthThird * 2, v, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);

        //middle left
        consumer.consume(quad(matrices, x, y + rHeightThird, rWidthThird, height - rHeightThird * 2, u, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //middle middle
        consumer.consume(quad(matrices, x + rWidthThird, y + rHeightThird, width - rWidthThird * 2, height - rHeightThird * 2, u + rWidthThird, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //middle right
        consumer.consume(quad(matrices, x + width - rWidthThird, y + rHeightThird, rWidthThird, height - rHeightThird * 2, u + rWidthThird * 2, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);

        //bottom left
        consumer.consume(quad(matrices, x, y + height - rHeightThird, rWidthThird, rHeightThird, u, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //bottom middle
        consumer.consume(quad(matrices, x + rWidthThird, y + height - rHeightThird, width - rWidthThird * 2, rHeightThird, u + rWidthThird, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //bottom right
        consumer.consume(quad(matrices, x + width - rWidthThird, y + height - rHeightThird, rWidthThird, rHeightThird, u + rWidthThird * 2, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
    }

    public static void renderTooltip(MatrixStack matrices, Text tooltip, Font f, int mouseX, int mouseY) {
        //variables
        int x = mouseX + 4;
        int y = mouseY - 12;
        int w = TextUtils.getWidth(tooltip, f);
        int h = TextUtils.getHeight(tooltip, f);

        //screen size
        Window window = Client.getInstance().window;
        int screenW = window.scaledWidth;
        int screenH = window.scaledHeight;

        //check if the tooltip could be rendered on the left side
        if (x + w > screenW && mouseX > screenW / 2)
            x -= 12 + w;

        //fit tooltip in the screen boundaries
        x = (int) Maths.clamp(x, -2f, screenW - w - 2);
        y = (int) Maths.clamp(y, -2f, screenH - h - 2);

        matrices.push();
        matrices.translate(x, y, 999f);

        //draw background
        nineQuad(VertexConsumer.GUI, matrices, TOOLTIP.getID(), 0, 0, w + 4, h + 4);

        //draw text
        f.render(VertexConsumer.FONT, matrices, 2, 2, tooltip);

        matrices.pop();
    }

    public static void setTooltip(Text text) {
        Screen s = Client.getInstance().screen;
        if (s != null) s.tooltip = text;
    }

    public static void setContextMenu(int x, int y, ContextMenu context) {
        Screen s = Client.getInstance().screen;
        if (s == null)
            return;

        ContextMenu sContext = s.contextMenu;
        if (sContext != null) {
            s.removeWidget(sContext);
            sContext.close();
        }

        if (context == null)
            return;

        context.setPos(x, y);
        fitToScreen(context);

        s.contextMenu = context;
        s.addWidgetOnTop(context);
    }

    public static boolean hasActiveContextMenu() {
        Screen s = Client.getInstance().screen;
        return s != null && s.contextMenu != null && s.contextMenu.isOpen();
    }

    public static void fitToScreen(Widget w) {
        //screen size
        Window window = Client.getInstance().window;
        int screenW = window.scaledWidth;
        int screenH = window.scaledHeight;

        //fix widget pos
        int x = (int) Maths.clamp(w.getX(), 0, screenW - w.getWidth());
        int y = (int) Maths.clamp(w.getY(), 0, screenH - w.getHeight());
        w.setPos(x, y);
    }
}
