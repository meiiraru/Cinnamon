package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import cinnamon.utils.UIHelper;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class ColorWheel extends SelectableWidget {

    private final Vector3f hsv = new Vector3f(0f, 1f, 1f);

    private boolean pressed;
    private boolean shift, ctrl, alt;
    private float altHue = 0f;

    private Consumer<Vector3f> updateListener, changeListener;

    public ColorWheel(int x, int y, int size) {
        super(x, y, size, size);
        setSelectable(false);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float radius = getRadius();
        int cx = getCenterX();
        int cy = getCenterY();

        //wheel
        Vertex[] vertices = GeometryHelper.circle(matrices, cx, cy, radius, 24, 0);
        vertices[0].color(0xFFFFFFFF);

        for (int i = 1; i < vertices.length; i++) {
            float hue = 1f - (i - 1f) / (vertices.length - 2f) - 90 / 360f;
            vertices[i].color(ColorUtils.hsvToRGB(new Vector3f(hue, 1f, 1f)));
        }

        VertexConsumer.MAIN.consume(vertices);

        //cross-hair
        matrices.pushMatrix();
        matrices.translate(0f, 0f, UIHelper.getDepthOffset());

        float angle = Math.toRadians(hsv.x * 360f);
        float r = hsv.y * radius;
        float x = cx + Math.cos(angle) * r;
        float y = cy + Math.sin(angle) * r;

        VertexConsumer.MAIN.consume(GeometryHelper.arc(matrices, x, y, 3, 0, 1, 1, 8, 0xFF000000));

        final int modColor = 0x44000000;

        //snap saturation line
        if (alt) {
            float x2 = cx + Math.cos(angle) * radius;
            float y2 = cy + Math.sin(angle) * radius;
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, cx, cy, x2, y2, 1, modColor));
        } else {
            //snap lines
            if (shift) {
                for (int i = 0; i < 16; i++) {
                    float ang = i * Math.PI_f / 8;
                    float x1 = cx + Math.cos(ang) * 3;
                    float y1 = cy + Math.sin(ang) * 3;
                    float x2 = cx + Math.cos(ang) * radius;
                    float y2 = cy + Math.sin(ang) * radius;
                    VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, x1, y1, x2, y2, 1, modColor));
                }
                VertexConsumer.MAIN.consume(GeometryHelper.arc(matrices, cx, cy, 3, 0, 1, 1, 24, modColor));
            }
            //snap hue circle
            if (ctrl)
                VertexConsumer.MAIN.consume(GeometryHelper.arc(matrices, cx, cy, r, 0, 1, 1, 24, modColor));
        }

        matrices.popMatrix();
    }

    public void setChangeListener(Consumer<Vector3f> changeListener) {
        this.changeListener = changeListener;
    }

    public void setUpdateListener(Consumer<Vector3f> updateListener) {
        this.updateListener = updateListener;
    }

    public void setColor(Vector3f hsv) {
        this.hsv.set(hsv);
        if (updateListener != null)
            updateListener.accept(hsv);
        if (changeListener != null)
            changeListener.accept(hsv);
    }

    public float getRadius() {
        return getWidth() / 2f - 4f;
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        shift = (mods & GLFW_MOD_SHIFT) != 0;
        ctrl = (mods & GLFW_MOD_CONTROL) != 0;
        boolean alt = (mods & GLFW_MOD_ALT) != 0;
        if (alt != this.alt) {
            this.alt = alt;
            altHue = hsv.x;
        }
        return super.keyPress(key, scancode, action, mods);
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (isHoveredOrFocused() && button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
            Client c = Client.getInstance();
            setColorAtPos(c.window.mouseX, c.window.mouseY);
            pressed = true;
            return this;
        }

        if (pressed) {
            pressed = false;
            if (changeListener != null)
                changeListener.accept(hsv);
        }

        return super.mousePress(button, action, mods);
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        if (pressed) {
            setColorAtPos(x, y);
            return this;
        }
        return super.mouseMove(x, y);
    }

    private void setColorAtPos(int x, int y) {
        int dx = x - getCenterX();
        int dy = y - getCenterY();
        float angle = Math.atan2(dy, -dx);
        if (shift && !alt) angle = Math.round(angle / (Math.PI_f / 8)) * Math.PI_f / 8;
        float hue = alt ? altHue : 1f - (angle + Math.PI_f) / Math.PI_TIMES_2_f;
        float sat = calculateSaturation(dx, dy, x, y);

        hsv.set(hue, sat, 1f);
        if (updateListener != null)
            updateListener.accept(hsv);
    }

    private float calculateSaturation(int dx, int dy, int x, int y) {
        float radius = getRadius();
        if (alt) {
            float cx = (x - getCenterX()) / radius;
            float cy = (y - getCenterY()) / radius;
            float ang = Math.toRadians(hsv.x * 360f);
            float px = Math.cos(ang);
            float py = Math.sin(ang);
            return Maths.clamp(cx * px + cy * py, 0f, 1f);
        }

        if (ctrl)
            return hsv.y;

        float r = Math.sqrt(dx * dx + dy * dy);
        return Math.min(r / radius, 1f);
    }
}
