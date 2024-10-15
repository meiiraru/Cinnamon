package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.GUIStyle;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.*;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.lwjgl.glfw.GLFW.*;

public class Slider extends SelectableWidget {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/slider.png");
    public static final BiFunction<Float, Integer, Text> DEFAULT_TOOLTIP = (f, i) -> Text.of(i);

    private float value = 0f;
    private int steps = 1;
    private float stepValue = 0.05f;
    private float scrollAmount = 0.05f;
    private int intValue = 0;
    private int min = 0;
    private int max = 100;
    private boolean vertical;
    private Integer color;
    private boolean showTooltip = true;
    private BiFunction<Float, Integer, Text> tooltipFunction = DEFAULT_TOOLTIP;
    private BiConsumer<Float, Integer> changeListener, updateListener;
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

        //left
        renderHorizontalBar(matrices, x, y, left, 0f, v, 0xFFFFFFFF);
        renderHorizontalBar(matrices, x, y, left, 0f, 12f, color == null ? GUIStyle.accentColor : color);

        //right
        renderHorizontalBar(matrices, x + width + 8 - right, y, right, 9f, v, 0xFFFFFFFF);

        //steps
        renderSteps(matrices, x, y, width, 3, 4, state * 3f + 9f, 12f);

        //button
        renderButton(matrices, x + left - 4, y - 2, state);
    }

    protected void renderHorizontalBar(MatrixStack matrices, int x, int y, int width, float u, float v, int color) {
        UIHelper.horizontalQuad(VertexConsumer.GUI, matrices, TEXTURE,
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

        //up
        renderVerticalBar(matrices, x, y, up, u, 0f, 0xFFFFFFFF);
        renderVerticalBar(matrices, x, y, up, 30f, 0f, color == null ? GUIStyle.accentColor : color);

        //down
        renderVerticalBar(matrices, x, y + height + 8 - down, down, u, 9f, 0xFFFFFFFF);

        //steps
        renderSteps(matrices, x, y, height, 4, 3, 30f, state * 3f + 9f);

        //button
        renderButton(matrices, x - 2, y + up - 4, state);
    }

    protected void renderVerticalBar(MatrixStack matrices, int x, int y, int height, float u, float v, int color) {
        UIHelper.verticalQuad(VertexConsumer.GUI, matrices, TEXTURE,
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
            ), TEXTURE);
        }
    }

    protected void renderButton(MatrixStack matrices, int x, int y, int state) {
        //button
        VertexConsumer.GUI.consume(GeometryHelper.quad(
                matrices, x, y, 8, 8,
                state * 8, 18f,
                8, 8,
                34, 26
        ), TEXTURE);

        //color
        Vertex[] vertices = GeometryHelper.quad(
                matrices, x, y, 8, 8,
                24f, 18f,
                8, 8,
                34, 26
        );
        int color = this.color == null ? GUIStyle.accentColor : this.color;
        for (Vertex vertex : vertices)
            vertex.color(color);

        VertexConsumer.GUI.consume(vertices, TEXTURE);
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        boolean wasSelected = mouseSelected;
        mouseSelected = isActive() && isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1;
        Window w = Client.getInstance().window;

        if (mouseSelected) {
            if (updateValueOnClick())
                updatePercentage(getValueAtMouse(w.mouseX, w.mouseY));

            anchorX = w.mouseX;
            anchorY = w.mouseY;
            anchorValue = getPercentage();

            return this;
        } else {
            updateHover(w.mouseX, w.mouseY);
            if (wasSelected)
                setPercentage(value); //just call the update to run the listeners
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

            updatePercentage(anchorValue + (float) delta / size);
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
        if (isActive() && isHoveredOrFocused()) {
            switch (key) {
                case GLFW_KEY_LEFT, GLFW_KEY_UP -> {return selectNext(true, action, mods);}
                case GLFW_KEY_RIGHT, GLFW_KEY_DOWN -> {return selectNext(false, action, mods);}
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }

    protected Slider selectNext(boolean backwards, int action, int mods) {
        if (action == GLFW_RELEASE) {
            setPercentage(value);
            return this;
        }

        if (steps == 1) {
            boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
            boolean ctrl = (mods & GLFW_MOD_CONTROL) != 0;
            int amount = shift || ctrl ? Math.max(getMax() / (shift ? 10 : 50), 1) : 1;
            updateValue(intValue + (backwards ? -amount : amount));
        } else {
            updatePercentage(value + (backwards ? -stepValue : stepValue));
        }

        return this;
    }

    public int getValue() {
        return this.intValue;
    }

    public void setValue(int value) {
        this.setPercentage(Maths.ratio(value, min, max));
    }

    public void updateValue(int value) {
        this.updatePercentage(Maths.ratio(value, min, max));
    }

    public float getPercentage() {
        return value;
    }

    public void setPercentage(float value) {
        updatePercentage(value);

        if (changeListener != null)
            changeListener.accept(this.value, this.intValue);
    }

    public void updatePercentage(float value) {
        //update value
        value = snapToClosestStep(value);
        value = Math.clamp(value, 0f, 1f);

        this.value = value;
        this.intValue = Math.round(Maths.lerp(min, max, value));

        if (showTooltip)
            super.setTooltip(tooltipFunction.apply(this.value, this.intValue));

        if (updateListener != null)
            updateListener.accept(this.value, this.intValue);
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
        updateValue(intValue);
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        updateValue(intValue);
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Colors color) {
        this.setColor(color.rgba);
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public boolean hasValueTooltip() {
        return showTooltip;
    }

    public void showValueTooltip(boolean bool) {
        showTooltip = bool;
        if (!showTooltip) super.setTooltip(null);
    }

    public void setTooltipFunction(BiFunction<Float, Integer, Text> tooltipFunction) {
        this.tooltipFunction = tooltipFunction;
        super.setTooltip(tooltipFunction.apply(this.value, this.intValue));
    }

    @Override
    public void setTooltip(Text tooltip) {
        showValueTooltip(false);
        super.setTooltip(tooltip);
    }

    public void setChangeListener(BiConsumer<Float, Integer> changeListener) {
        this.changeListener = changeListener;
    }

    public void setUpdateListener(BiConsumer<Float, Integer> updateListener) {
        this.updateListener = updateListener;
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

    protected float getValueAtMouse(int mouseX, int mouseY) {
        int pos;
        int size;

        if (isVertical()) {
            pos = mouseY - getY() - handleSize / 2;
            size = getHeight() - handleSize;
        } else {
            pos = mouseX - getX() - handleSize / 2;
            size = getWidth() - handleSize;
        }

        float value = (float) pos / size;
        value = snapToClosestStep(value);
        return Math.clamp(value, 0f, 1f);
    }

    protected boolean updateValueOnClick() {
        return true;
    }

    @Override
    public void renderTooltip(MatrixStack matrices, Font font) {
        if (!showTooltip || !isHovered()) {
            super.renderTooltip(matrices, font);
            return;
        }

        //grab text
        Window window = Client.getInstance().window;
        float value = getValueAtMouse(window.mouseX, window.mouseY);
        Text tooltip = tooltipFunction.apply(value, Math.round(Maths.lerp(min, max, value)));

        if (tooltip == null || tooltip.isEmpty())
            return;

        //dimensions
        int w = TextUtils.getWidth(tooltip, font);
        int h = TextUtils.getHeight(tooltip, font);

        int wx = getX();
        int wy = getY();
        int cx = getCenterX();
        int cy = getCenterY();

        int screenW = window.scaledWidth;
        int screenH = window.scaledHeight;

        int b = GUIStyle.tooltipBorder;


        if (isVertical()) {
            int animY = (int) ((getHeight() - handleSize) * value) + handleSize / 2;
            boolean left = false;
            int x = wx + getWidth() + b + 4;
            int y = wy - h / 2 + animY;

            //boundaries test
            if (x + w + b > screenW && cx > screenW / 2) {
                x = wx - w - b - 4;
                left = true;
            }
            x = Math.clamp(x, b, screenW - w - b);
            y = Math.clamp(y, b, screenH - h - b);

            //render
            UIHelper.renderTooltip(matrices, x, y, w, h, cx, wy + animY, (byte) (left ? 1 : 0), tooltip, font);
        } else {
            int animX = (int) ((getWidth() - handleSize) * value) + handleSize / 2;
            boolean bottom = false;
            int x = wx - w / 2 + animX;
            int y = wy - h - b - 4;

            //boundaries test
            if (y < b && cy < screenH / 2) {
                y = wy + (int) font.lineHeight + b + 4;
                bottom = true;
            }
            x = Math.clamp(x, b, screenW - w - b);
            y = Math.clamp(y, b, screenH - h - b);

            //render
            UIHelper.renderTooltip(matrices, x, y, w, h, wx + animX, cy, (byte) (bottom ? 3 : 2), tooltip, font);
        }
    }
}
