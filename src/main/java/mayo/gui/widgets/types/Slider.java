package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.SelectableWidget;
import mayo.model.GeometryHelper;
import mayo.model.Vertex;
import mayo.render.MatrixStack;
import mayo.render.texture.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.utils.UIHelper;

import java.util.function.BiConsumer;

import static org.lwjgl.glfw.GLFW.*;

public class Slider extends SelectableWidget {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/slider.png"));

    private float value = 0f;
    private int steps = 1;
    private float stepValue = 0.05f;
    private float scrollAmount = 0.05f;
    private int intValue = 0;
    private int min = 0;
    private int max = 100;
    private boolean vertical;
    private int color = -1;
    private boolean showTooltip = true;
    private BiConsumer<Float, Integer> changeListener;
    private boolean mouseSelected;
    private float animationValue;

    protected int handleSize = 8;
    protected int anchorX, anchorY;
    protected float anchorValue;

    public Slider(int x, int y, int size) {
        this(x, y, size, 8);
    }

    protected Slider(int x, int y, int width, int height) {
        super(x, y, width, height);
        setValue(0);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float d = UIHelper.tickDelta(0.4f);
        animationValue = Maths.lerp(animationValue, value, d);

        if (isVertical())
            renderVertical(matrices, mouseX, mouseY, delta);
        else
            renderHorizontal(matrices, mouseX, mouseY, delta);
    }

    protected void renderHorizontal(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY() + 2;
        int width = getWidth() - 8;
        float val = getAnimationValue();
        int state = getState();

        int left = Math.round((width * val) + 4);
        int right = Math.round((width * (1 - val)) + 4);

        int v = state * 4;
        int id = TEXTURE.getID();

        //left
        renderHorizontalBar(matrices, x, y, left, 0f, v, id, -1);
        renderHorizontalBar(matrices, x, y, left, 0f, 12f, id, color);

        //right
        renderHorizontalBar(matrices, x + width + 8 - right, y, right, 9f, v, id, -1);

        //steps
        renderSteps(matrices, x, y, width, 3, 4, state * 3f + 9f, 12f);

        //button
        renderButton(matrices, x + left - 4, y - 2, state, id);
    }

    protected void renderHorizontalBar(MatrixStack matrices, int x, int y, int width, float u, float v, int id, int color) {
        UIHelper.horizontalQuad(VertexConsumer.GUI, matrices, id,
                x, y, width, 4,
                u, v,
                9, 4,
                34, 26,
                color
        );
    }

    protected void renderVertical(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int x = getX() + 2;
        int y = getY();
        int height = getHeight() - 8;
        float val = getAnimationValue();
        int state = getState();

        int up = Math.round((height * val) + 4);
        int down = Math.round((height * (1 - val)) + 4);

        int u = state * 4 + 18;
        int id = TEXTURE.getID();

        //up
        renderVerticalBar(matrices, x, y, up, u, 0f, id, -1);
        renderVerticalBar(matrices, x, y, up, 30f, 0f, id, color);

        //down
        renderVerticalBar(matrices, x, y + height + 8 - down, down, u, 9f, id, -1);

        //steps
        renderSteps(matrices, x, y, height, 4, 3, 30f, state * 3f + 9f);

        //button
        renderButton(matrices, x - 2, y + up - 4, state, id);
    }

    protected void renderVerticalBar(MatrixStack matrices, int x, int y, int height, float u, float v, int id, int color) {
        UIHelper.verticalQuad(VertexConsumer.GUI, matrices, id,
                x, y, 4, height,
                u, v,
                4, 9,
                34, 26,
                color
        );
    }

    protected void renderSteps(MatrixStack matrices, int x, int y, int length, int width, int height, float u, float v) {
        if (steps <= 1)
            return;

        int id = TEXTURE.getID();
        boolean vertical = isVertical();
        int x2 = x;
        int y2 = y;

        for (int i = 0; i < steps; i++) {
            int pos = Math.round(length * (stepValue * i) + 4 - 1.5f);
            if (vertical) y2 = y + pos;
            else x2 = x + pos;

            VertexConsumer.GUI.consume(GeometryHelper.quad(
                    matrices, x2, y2, width, height,
                    u, v,
                    width, height,
                    34, 26
            ), id);
        }
    }

    protected void renderButton(MatrixStack matrices, int x, int y, int state, int id) {
        //button
        VertexConsumer.GUI.consume(GeometryHelper.quad(
                matrices, x, y, 8, 8,
                state * 8, 18f,
                8, 8,
                34, 26
        ), id);

        //color
        Vertex[] vertices = GeometryHelper.quad(
                matrices, x, y, 8, 8,
                24f, 18f,
                8, 8,
                34, 26
        );
        for (Vertex vertex : vertices)
            vertex.color(color);

        VertexConsumer.GUI.consume(vertices, id);
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        mouseSelected = isActive() && isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1;
        Window w = Client.getInstance().window;

        if (mouseSelected) {
            if (!isHandleHovered()) {
                int pos;
                int size;

                if (isVertical()) {
                    pos = w.mouseY - getY() - handleSize / 2;
                    size = getHeight() - handleSize;
                } else {
                    pos = w.mouseX - getX() - handleSize / 2;
                    size = getWidth() - handleSize;
                }

                setPercentage((float) pos / size);
            }

            anchorX = w.mouseX;
            anchorY = w.mouseY;
            anchorValue = getPercentage();

            return this;
        } else {
            updateHover(w.mouseX, w.mouseY);
        }

        return super.mousePress(button, action, mods);
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        if (this.mouseSelected) {
            int delta;
            int size;

            if (isVertical()) {
                delta = y - anchorY;
                size = getHeight() - handleSize;
            } else {
                delta = x - anchorX;
                size = getWidth() - handleSize;
            }

            setPercentage(anchorValue + (float) delta / size);
            return this;
        }

        return super.mouseMove(x, y);
    }

    @Override
    public GUIListener scroll(double x, double y) {
        if (isActive() && isHovered()) {
            return forceScroll(x, y);
        }
        return super.scroll(x, y);
    }

    public GUIListener forceScroll(double x, double y) {
        float val = steps == 1 ? scrollAmount : stepValue;
        setPercentage(value + (Math.signum(-y) < 0 ? -val : val));
        return this;
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (isActive() && isHoveredOrFocused() && action != GLFW_RELEASE) {
            switch (key) {
                case GLFW_KEY_LEFT, GLFW_KEY_UP -> {return selectNext(true, mods);}
                case GLFW_KEY_RIGHT, GLFW_KEY_DOWN -> {return selectNext(false, mods);}
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }

    protected Slider selectNext(boolean backwards, int mods) {
        if (steps == 1) {
            boolean shift = (mods & GLFW_MOD_SHIFT) == GLFW_MOD_SHIFT;
            boolean ctrl = (mods & GLFW_MOD_CONTROL) == GLFW_MOD_CONTROL;
            int amount = shift || ctrl ? Math.max(getMax() / (shift ? 10 : 50), 1) : 1;
            setValue(intValue + (backwards ? -amount : amount));
        } else {
            setPercentage(value + (backwards ? -stepValue : stepValue));
        }

        return this;
    }

    public int getValue() {
        return this.intValue;
    }

    public void setValue(int value) {
        this.setPercentage((float) value / max);
    }

    public float getPercentage() {
        return value;
    }

    public void setPercentage(float value) {
        //update value
        value = snapToClosestStep(value);
        value = Math.clamp(value, 0f, 1f);

        this.value = value;
        this.intValue = Math.round((max - min) * value + min);

        //listeners
        if (changeListener != null)
            changeListener.accept(this.value, this.intValue);

        if (showTooltip)
            super.setTooltip(Text.of(this.intValue));
    }

    protected float snapToClosestStep(float value) {
        if (steps == 1)
            return value;

        int mul = Math.round((value - min) / stepValue);
        return stepValue * mul + min;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        if (this.vertical != vertical) {
            setDimensions(getHeight(), getWidth());
            this.vertical = vertical;
        }
    }

    public int getStepCount() {
        return steps;
    }

    public void setStepCount(int steps) {
        this.steps = Math.max(steps, 1);
        this.stepValue = this.steps > 1 ? (1f / (this.steps - 1)) : 0.05f;
        setPercentage(value);
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
        setValue(intValue);
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        setValue(intValue);
    }

    public int getColor() {
        return color;
    }

    public void setColor(Colors color) {
        this.setColor(color.rgba);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean hasValueTooltip() {
        return showTooltip;
    }

    public void showValueTooltip(boolean bool) {
        showTooltip = bool;
        if (!showTooltip) super.setTooltip(null);
    }

    @Override
    public void setTooltip(Text tooltip) {
        showValueTooltip(false);
        super.setTooltip(tooltip);
    }

    public void setChangeListener(BiConsumer<Float, Integer> changeListener) {
        this.changeListener = changeListener;
    }

    public float getAnimationValue() {
        return animationValue;
    }

    public int getStepIndex() {
        return steps == 1 ? intValue : Math.round(value / stepValue);
    }

    public boolean isDragged() {
        return mouseSelected;
    }

    public void setScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
    }

    public boolean isHandleHovered() {
        if (!super.isHovered())
            return false;

        Window w = Client.getInstance().window;
        if (isVertical()) {
            int y = getY() + Math.round((getHeight() - handleSize) * getPercentage());
            return w.mouseY >= y && w.mouseY < y + handleSize;
        } else {
            int x = getX() + Math.round((getWidth() - handleSize) * getPercentage());
            return w.mouseX >= x && w.mouseX < x + handleSize;
        }
    }
}
