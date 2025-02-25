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
import cinnamon.utils.UIHelper;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

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
        private boolean pressed;

        public ColorWheel(int x, int y, int size, ColorPicker parent) {
            super(x, y, size, size);
            this.parent = parent;
            setSelectable(false);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            float radius = getWidth() / 2f - 4f;

            Vertex[] vertices = GeometryHelper.circle(matrices, getCenterX(), getCenterY(), radius, 24, 0);
            vertices[0].color(0xFFFFFFFF);

            for (int i = 1; i < vertices.length; i++) {
                float hue = 1f - (i - 1f) / (vertices.length - 2f) - 90 / 360f;
                vertices[i].color(ColorUtils.hsvToRGB(new Vector3f(hue, 1f, 1f)));
            }

            VertexConsumer.GUI.consume(vertices);

            Vector3f hsv = ColorUtils.rgbToHSV(ColorUtils.intToRGB(parent.getColor()));
            float angle = (float) Math.toRadians(hsv.x * 360f);
            float r = hsv.y * radius;
            float x = getCenterX() + (float) Math.cos(angle) * r;
            float y = getCenterY() + (float) Math.sin(angle) * r;

            VertexConsumer.GUI.consume(GeometryHelper.circle(matrices, x, y, 3, 8, 0x88000000));
            VertexConsumer.GUI.consume(GeometryHelper.circle(matrices, x, y, 2, 8, 0xFFFFFFFF));
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
            float radius = getWidth() / 2f - 4f;
            float dx = x - getCenterX();
            float dy = y - getCenterY();
            float angle = (float) Math.atan2(dy, -dx);
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            float hue = 1f - (angle + (float) Math.PI) / (2f * (float) Math.PI);
            float sat = Math.min(1f, r / radius);

            parent.setColor(ColorUtils.rgbToInt(ColorUtils.hsvToRGB(new Vector3f(hue, sat, 1f))) + 0xFF000000);
        }
    }
}
