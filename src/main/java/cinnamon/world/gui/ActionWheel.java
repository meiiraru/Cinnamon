package cinnamon.world.gui;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.settings.Settings;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import org.joml.Math;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ActionWheel extends Overlay {

    public static final Style
            STYLE = Style.EMPTY.outlined(true),
            ERROR_STYLE = STYLE.color(Colors.RED);
    private static final Text
            NO_ACTIONS_TEXT = Text.translated("gui.overlay.action_wheel.no_actions").withStyle(ERROR_STYLE),
            CANCEL_TEXT = Text.translated("gui.overlay.action_wheel.cancel").withStyle(ERROR_STYLE);

    public static final int
            RADIUS = 80,
            INNER_RADIUS = 40,
            SIDES = 32;
    public static final float
            PADDING_W = 1f,
            PADDING_H = 1f;

    protected List<Action> actions = new ArrayList<>();
    protected int selected = -1;

    public boolean renderDebug = false;

    public ActionWheel() {
        this.stealMouse = true;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(MatrixStack matrices, float delta) {
        super.render(matrices, delta);

        //prepare variables
        Window window = Client.getInstance().window;
        int w = window.getGUIWidth() / 2;
        int h = window.getGUIHeight() / 2;

        int mouseX = window.mouseX - w;
        int mouseY = window.mouseY - h;

        matrices.pushMatrix();
        matrices.translate(w, h, 0);

        //render action wheel background
        VertexConsumer.MAIN.consume(GeometryHelper.arc(matrices, 0, 0, RADIUS, 0f, 1f, INNER_RADIUS, SIDES, 0x88000000));

        //render debug mouse line
        if (renderDebug)
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, 0, 0, mouseX, mouseY, 0.5f, 0xFFFFFFFF));

        //no actions...
        int len = this.actions.size();
        if (len == 0) {
            NO_ACTIONS_TEXT.render(VertexConsumer.MAIN, matrices, 0, 0, Alignment.CENTER);
            matrices.popMatrix();
            selected = -1;
            return;
        }

        //check if the mouse is inside the inner radius
        int dist = mouseX * mouseX + mouseY * mouseY;
        boolean cancel = dist < INNER_RADIUS * INNER_RADIUS;

        //set selected
        if (cancel) {
            selected = -1;
        } else {
            float angle = Math.atan2(mouseY, mouseX) + Math.PI_OVER_2_f;
            angle = (angle + Math.PI_TIMES_2_f) % Math.PI_TIMES_2_f;
            selected = (int) Math.floor(angle / Math.PI_TIMES_2_f * len);
        }

        //render each action
        float paddingW = len > 1 ? ActionWheel.PADDING_W : 0f;
        float anglePerAction = Math.toDegrees(Math.PI_TIMES_2_f / len);

        for (int i = 0; i < len; i++) {
            Action action = actions.get(i);
            action.setHovered(i == selected);

            //segment
            float startAngle = (anglePerAction * i + paddingW * 0.5f) / 360f;
            float endAngle = (anglePerAction * (i + 1) - paddingW * 0.5f) / 360f;
            int color = action.getColor();
            VertexConsumer.MAIN.consume(GeometryHelper.arc(matrices, 0, 0, RADIUS - PADDING_H, startAngle, endAngle, INNER_RADIUS - PADDING_H - PADDING_H, SIDES, color));

            //translate matrix to the center of the segment
            float midAngle = (startAngle + endAngle) * Math.PI_f - Math.PI_OVER_2_f;
            float x = Math.cos(midAngle) * (RADIUS + INNER_RADIUS) * 0.5f;
            float y = Math.sin(midAngle) * (RADIUS + INNER_RADIUS) * 0.5f;

            //render the action
            action.render(matrices, x, y, delta);
        }

        //text
        if (cancel)
            CANCEL_TEXT.render(VertexConsumer.MAIN, matrices, 0, 0, Alignment.CENTER);
        else if (actions.get(selected).getFormattedTitle() != null)
            actions.get(selected).getFormattedTitle().render(VertexConsumer.MAIN, matrices, 0, 0, Alignment.CENTER);

        matrices.popMatrix();
    }

    public ActionWheel addAction(Action action) {
        this.actions.add(action);
        return this;
    }

    public ActionWheel runAction(int index) {
        if (index >= 0 && index < actions.size())
            actions.get(index).run();
        return this;
    }

    public ActionWheel runSelected() {
        if (selected != -1)
            return this.runAction(selected);
        return this;
    }

    public ActionWheel removeAction(Action action) {
        return this.removeAction(actions.indexOf(action));
    }

    public ActionWheel removeAction(int index) {
        if (index >= 0 && index < actions.size())
            actions.remove(index);
        return this;
    }

    public ActionWheel clear() {
        this.actions.clear();
        return this;
    }

    @Override
    public void open() {
        if (!isClosed())
            return;

        this.selected = -1;
        super.open();
    }

    @Override
    public void close() {
        if (isClosed())
            return;

        if (Settings.actionWheelRunOnClose.get()) {
            this.runSelected();
            this.selected = -1;
        }
        super.close();
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS && this.selected != -1) {
            this.runSelected();
            return true;
        }
        return super.mousePress(button, action, mods);
    }
}
