package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.UIHelper;
import org.joml.Math;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class VecScreen extends ParentedScreen {

    private final List<Vector2f> vectors = new ArrayList<>();
    private String xBuffer = "", buffer = "";
    private float scale = 5f;

    public VecScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        matrices.pushMatrix();
        matrices.translate(width / 2f, height / 2f, 0f);

        //draw xy axis
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, -width / 2f, 0, width / 2f, 0, 1f, Colors.WHITE.argb));
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, 0, -height / 2f, 0, height / 2f, 1f, Colors.WHITE.argb));

        matrices.scale(scale, scale, 1f);

        Vector2f src = new Vector2f(0, 0);

        matrices.pushMatrix();
        float d = UIHelper.getDepthOffset();

        for (int i = 0; i < vectors.size(); i++) {
            matrices.translate(0f, 0f, d);
            Vector2f vec = vectors.get(i);
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, src.x, -src.y, src.x + vec.x, -src.y - vec.y, 3f / scale, (i % 2 == 0 ? Colors.YELLOW : Colors.ORANGE).argb));
            src.add(vec);
        }

        matrices.translate(0f, 0f, d);
        VertexConsumer.MAIN.consume(
                GeometryHelper.circle(matrices, src.x, -src.y, 3f / scale, 16, Colors.LIME.argb));

        matrices.popMatrix();
        matrices.popMatrix();

        //draw texts
        matrices.pushMatrix();
        matrices.translate(0f, 0f, d * (vectors.size() + 2));

        Text.of("X: " + (xBuffer.isEmpty() ? buffer + "_" : xBuffer) + " Y: " + (xBuffer.isEmpty() ? "" : buffer + "_"))
                .withStyle(Style.EMPTY.outlined(true))
                .render(VertexConsumer.MAIN, matrices, width / 2f, 4, Alignment.TOP_CENTER);

        Text.of("x " + src.x + " y " + src.y + " | scale " + String.format("%.2f", scale))
                .withStyle(Style.EMPTY.outlined(true))
                .render(VertexConsumer.MAIN, matrices, 4, height - 4, Alignment.BOTTOM_LEFT);

        //axis labels
        for (int i = 1; i <= 10; i++) {
            Text.of( i * 10).render(VertexConsumer.MAIN, matrices, width / 2f + i * 10 * scale, height / 2f + 4, Alignment.TOP_CENTER);
            Text.of(-i * 10).render(VertexConsumer.MAIN, matrices, width / 2f - i * 10 * scale, height / 2f + 4, Alignment.TOP_CENTER);
            Text.of( i * 10).render(VertexConsumer.MAIN, matrices, width / 2f + 4, height / 2f - i * 10 * scale, Alignment.CENTER_LEFT);
            Text.of(-i * 10).render(VertexConsumer.MAIN, matrices, width / 2f + 4, height / 2f + i * 10 * scale, Alignment.CENTER_LEFT);
        }

        //if mouse is inside the "src" circle
        if (mouseX > width / 2f + (src.x - 3f / scale) * scale && mouseX < width / 2f + (src.x + 3f / scale) * scale &&
                mouseY > height / 2f + (-src.y - 3f / scale) * scale && mouseY < height / 2f + (-src.y + 3f / scale) * scale)
            UIHelper.renderTooltip(matrices, mouseX, mouseY, Text.of("(" + src.x + ", " + src.y + ")"));

        matrices.popMatrix();

        super.renderChildren(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (c == 'x' && !vectors.isEmpty()) {
            vectors.removeLast();
            return true;
        }

        if (Character.isDigit(c) || (c == '-' && buffer.isEmpty()) || (c == '.' && !buffer.contains("."))) {
            buffer += c;
            return true;
        }

        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !buffer.isEmpty()) {
                buffer = buffer.substring(0, buffer.length() - 1);
                return true;
            } else if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !buffer.isEmpty()) {
                if (xBuffer.isEmpty()) {
                    xBuffer = buffer;
                } else {
                    vectors.add(new Vector2f(Float.parseFloat(xBuffer), Float.parseFloat(buffer)));
                    xBuffer = "";
                }
                buffer = "";
                return true;
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }

    @Override
    public boolean scroll(double x, double y) {
        if (y != 0) {
            scale *= (y > 0 ? 1.1f : 0.9f);
            scale = Math.max(0.1f, scale);
            return true;
        }

        return super.scroll(x, y);
    }
}
