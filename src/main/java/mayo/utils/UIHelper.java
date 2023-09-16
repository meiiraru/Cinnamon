package mayo.utils;

import mayo.Client;
import mayo.gui.widgets.Widget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;

import static mayo.model.GeometryHelper.quad;

public class UIHelper {

    private static final Texture[] BACKGROUND = new Texture[]{
            new Texture(new Resource("textures/gui/background/background_0.png")),
            new Texture(new Resource("textures/gui/background/background_1.png")),
            new Texture(new Resource("textures/gui/background/background_2.png"))
    };
    private static final Texture TOOLTIP = new Texture(new Resource("textures/gui/tooltip.png"));

    public static void renderBackground(MatrixStack matrices, int width, int height, float delta) {
        Client c = Client.getInstance();
        float speed = 0.125f;
        int textureSize = 64;
        width += textureSize;
        height += textureSize;

        for (int i = 0; i < BACKGROUND.length; i++) {
            float x = 0, y = 0;
            float d = (c.ticks + delta) * speed;

            x -= d % textureSize;
            y -= d % textureSize;

            float u1 = (float) width / textureSize;
            float v1 = (float) height / textureSize;

            VertexConsumer.GUI.consume(quad(
                    x, y,
                    width, height,
                    -999,
                    0f, u1,
                    0f, v1
            ), BACKGROUND[i].getID());

            speed *= 2f;
        }
    }

    public static boolean isMouseOver(Widget w, int mouseX, int mouseY) {
        return isMouseOver(w.getX(), w.getY(), w.getWidth(), w.getHeight(), mouseX, mouseY);
    }

    public static boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void nineQuad(VertexConsumer vertexConsumer, int textureID, float x, float y, float width, float height) {
        nineQuad(vertexConsumer, textureID, x, y, width, height, 0f, 0f, 15, 15, 15, 15);
    }

    public static void nineQuad(VertexConsumer consumer, int textureID, float x, float y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        float rWidthThird = regionWidth / 3f;
        float rHeightThird = regionHeight / 3f;

        //top left
        consumer.consume(quad(x, y, rWidthThird, rHeightThird, u, v, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //top middle
        consumer.consume(quad(x + rWidthThird, y, width - rWidthThird * 2, rHeightThird, u + rWidthThird, v, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //top right
        consumer.consume(quad(x + width - rWidthThird, y, rWidthThird, rHeightThird, u + rWidthThird * 2, v, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);

        //middle left
        consumer.consume(quad(x, y + rHeightThird, rWidthThird, height - rHeightThird * 2, u, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //middle middle
        consumer.consume(quad(x + rWidthThird, y + rHeightThird, width - rWidthThird * 2, height - rHeightThird * 2, u + rWidthThird, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //middle right
        consumer.consume(quad(x + width - rWidthThird, y + rHeightThird, rWidthThird, height - rHeightThird * 2, u + rWidthThird * 2, v + rHeightThird, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);

        //bottom left
        consumer.consume(quad(x, y + height - rHeightThird, rWidthThird, rHeightThird, u, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //bottom middle
        consumer.consume(quad(x + rWidthThird, y + height - rHeightThird, width - rWidthThird * 2, rHeightThird, u + rWidthThird, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
        //bottom right
        consumer.consume(quad(x + width - rWidthThird, y + height - rHeightThird, rWidthThird, rHeightThird, u + rWidthThird * 2, v + rHeightThird * 2, rWidthThird, rHeightThird, textureWidth, textureHeight), textureID);
    }

    public static void renderTooltip(MatrixStack matrices, Text tooltip, Font f, int mouseX, int mouseY) {
        //variables
        int x = mouseX + 4;
        int y = mouseY - 12;
        float w = f.width(tooltip);
        float h = f.height(tooltip);

        //screen size
        Window window = Client.getInstance().window;
        int screenW = window.scaledWidth;
        int screenH = window.scaledHeight;

        //check if the tooltip could be rendered on the left side
        if (x + w > screenW && mouseX > screenW / 2)
            x -= (int) (12 + w);

        //fit tooltip in the screen boundaries
        x = (int) Meth.clamp(x, -2f, screenW - w - 2);
        y = (int) Meth.clamp(y, -2f, screenH - h - 2);

        matrices.push();
        matrices.translate(x + 2, y + 2, 999f);

        //draw background
        nineQuad(VertexConsumer.GUI, TOOLTIP.getID(), x, y, w + 4, h + 4);

        //draw text
        f.render(VertexConsumer.FONT, matrices.peek(), tooltip);

        matrices.pop();
    }
}
