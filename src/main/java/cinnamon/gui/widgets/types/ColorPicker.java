package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.UIHelper;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class ColorPicker extends Button {

    private int color = 0xFFFFFFFF; //ARGB
    private boolean customTooltip = false;
    private final PopupWidget picker;

    public ColorPicker(int x, int y, int width, int height) {
        super(x, y, width, height, null, b -> ((ColorPicker) b).openPicker());

        setTooltip(null);

        picker = new PopupWidget(0, 0, 4, 2) {
            @Override
            protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                super.renderWidget(matrices, mouseX, mouseY, delta);
                //render background
                UIHelper.nineQuad(VertexConsumer.GUI, matrices, getStyle().colorPickerTex, getAlignedX(), getAlignedY(), getWidth(), getHeight(), 32, 0, 16, 16, 48, 16);
            }
        };
        picker.closeOnSelect(false);
        picker.setAlignment(Alignment.CENTER);
        picker.addWidget(new ColorWheel(0, 0, 64, this));
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(VertexConsumer.GUI, matrices, getStyle().colorPickerTex, getX(), getY(), getWidth(), getHeight(), 16, 0, 16, 16, 48, 16, color);
        UIHelper.nineQuad(VertexConsumer.GUI, matrices, getStyle().colorPickerTex, getX(), getY(), getWidth(), getHeight(), 0, 0, 16, 16, 48, 16);
    }

    @Override
    public void setTooltip(Text tooltip) {
        customTooltip = tooltip != null;
        if (!customTooltip) {
            super.setTooltip(Text.of("#" + ColorUtils.rgbToHex(ColorUtils.intToRGB(color))));
        } else {
            super.setTooltip(tooltip);
        }
    }

    public int getColor() {
        return color;
    }

    public void setColor(Colors color) {
        setColor(color.rgba);
    }

    public void setColor(int color) {
        this.color = color;
        if (!customTooltip)
            setTooltip(null);
    }

    protected void openPicker() {
        UIHelper.setPopup(getCenterX(), getCenterY(), picker);
        picker.open();
    }

    private static class ColorWheel extends SelectableWidget {

        private final ColorPicker parent;
        private final Vector3f hsv = new Vector3f(0f, 1f, 1f);
        private boolean pressed;
        private boolean shift, ctrl, alt;
        private float altHue = 0f;

        public ColorWheel(int x, int y, int size, ColorPicker parent) {
            super(x, y, size, size);
            this.parent = parent;
            setSelectable(false);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            float radius = getWidth() / 2f - 4f;
            int cx = getCenterX();
            int cy = getCenterY();

            //wheel
            Vertex[] vertices = GeometryHelper.circle(matrices, cx, cy, radius, 24, 0);
            vertices[0].color(0xFFFFFFFF);

            for (int i = 1; i < vertices.length; i++) {
                float hue = 1f - (i - 1f) / (vertices.length - 2f) - 90 / 360f;
                vertices[i].color(ColorUtils.hsvToRGB(new Vector3f(hue, 1f, 1f)));
            }

            VertexConsumer.GUI.consume(vertices);

            //cross-hair
            float angle = (float) Math.toRadians(hsv.x * 360f);
            float r = hsv.y * radius;
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;

            VertexConsumer.GUI.consume(GeometryHelper.arc(matrices, x, y, 3, 0, 1, 1, 8, 0xFF000000));

            //snap lines
            if (shift) {
                for (int i = 0; i < 16; i++) {
                    float ang = i * (float) Math.PI / 8;
                    float x1 = cx + (float) Math.cos(ang) * 3;
                    float y1 = cy + (float) Math.sin(ang) * 3;
                    float x2 = cx + (float) Math.cos(ang) * radius;
                    float y2 = cy + (float) Math.sin(ang) * radius;
                    VertexConsumer.GUI.consume(GeometryHelper.arc(matrices, cx, cy, 3, 0, 1, 1, 24, 0xFF000000));
                    VertexConsumer.GUI.consume(GeometryHelper.line(matrices, x1, y1, x2, y2, 1, 0xFF000000));
                }
            }
            //snap hue circle
            if (ctrl)
                VertexConsumer.GUI.consume(GeometryHelper.arc(matrices, cx, cy, r, 0, 1, 1, 24, 0xFF000000));
            //snap saturation line
            if (alt) {
                float x2 = cx + (float) Math.cos(angle) * radius;
                float y2 = cy + (float) Math.sin(angle) * radius;
                VertexConsumer.GUI.consume(GeometryHelper.line(matrices, cx, cy, x2, y2, 1, 0xFF000000));
            }
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
            pressed = false;

            if (isHoveredOrFocused() && button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                Client c = Client.getInstance();
                setColorAtPos(c.window.mouseX, c.window.mouseY);
                pressed = true;
                return this;
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
            float angle = (float) Math.atan2(dy, -dx);
            if (shift) angle = (float) Math.round(angle / (Math.PI / 8)) * (float) Math.PI / 8;
            float hue = alt ? altHue : 1f - (angle + (float) Math.PI) / (2f * (float) Math.PI);
            float sat = calculateSaturation(dx, dy, x, y);

            hsv.set(hue, sat, 1f);
            parent.setColor(ColorUtils.rgbToInt(ColorUtils.hsvToRGB(hsv)) + 0xFF000000);
        }

        private float calculateSaturation(int dx, int dy, int x, int y) {
            if (ctrl)
                return hsv.y;

            float radius = getWidth() / 2f - 4f;
            if (alt) {
                int cx = getCenterX();
                int cy = getCenterY();
                float ang = (float) Math.toRadians(hsv.x * 360f);
                float x2 = (cx + (float) Math.cos(ang) * radius) - cx;
                float y2 = (cy + (float) Math.sin(ang) * radius) - cy;
                return Maths.clamp((x2 * (x - cx) + y2 * (y - cy)) / (x2 * x2 + y2 * y2), 0f, 1f);
            }

            float r = (float) Math.sqrt(dx * dx + dy * dy);
            return Math.min(r / radius, 1f);
        }
    }
}
