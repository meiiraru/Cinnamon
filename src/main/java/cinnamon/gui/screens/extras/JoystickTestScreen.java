package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.Widget;
import cinnamon.gui.widgets.types.Button;
import cinnamon.input.Keybind;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;
import org.joml.Math;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class JoystickTestScreen extends ParentedScreen {

    protected final List<Keybind> keybinds = new ArrayList<>();

    public JoystickTestScreen(Screen parentScreen) {
        super(parentScreen);
        //register gamepad keybinds
        for (int i = 0; i < 15; i++) {
            Keybind keybind = new Keybind("gamepad_button_" + i, i, 0, Keybind.KeyType.GAMEPAD_BUTTON, 0);
            keybinds.add(keybind);
        }
        for (int i = 0; i < 6; i++) {
            Keybind keybind = new Keybind("gamepad_axis_" + i, i, 0, Keybind.KeyType.GAMEPAD_AXIS, 0);
            keybinds.add(keybind);
        }
    }

    @Override
    public void init() {
        super.init();

        //center of the screen
        int centerX = width / 2;
        int centerY = height / 2;

        //add the hat buttons
        //up - right - down - left
        int x = centerX - 130;
        int y = centerY;
        float r = 24;

        for (int i = 0; i < 4; i++) {
            Keybind keybind = keybinds.get(i + 11);
            float angle = Math.toRadians(i * 90 - 90);
            int buttonX = (int) (x + r * Math.cos(angle));
            int buttonY = (int) (y + r * Math.sin(angle));
            addWidget(new JoystickButton(buttonX, buttonY, 20, 20, keybind, false));
        }

        //add abxy buttons
        //down - right - left - up
        x = centerX + 110;
        for (int i = 0; i < 4; i++) {
            Keybind keybind = keybinds.get(i);
            float angle = Math.toRadians(i < 2 ? i * -90 + 90 : i * 90);
            int buttonX = (int) (x + r * Math.cos(angle));
            int buttonY = (int) (y + r * Math.sin(angle));
            addWidget(new JoystickButton(buttonX, buttonY, 20, 20, keybind, true));
        }

        //back and start
        x = centerX - 40;
        y = centerY - 10;
        for (int i = 0; i < 2; i++) {
            Keybind keybind = keybinds.get(i + 6);
            addWidget(new JoystickButton(x + i * 50, y, 30, 12, keybind, false));
        }

        //lb
        y = centerY - 4 - 20 - 16 - 20;
        addWidget(new JoystickButton(centerX - 150, y, 60, 20, keybinds.get(4), false));
        //rb
        addWidget(new JoystickButton(centerX + 90, y, 60, 20, keybinds.get(5), false));

        //lt
        y = centerY - 4 - 20 - 16 - 20 - 10 - 20;
        addWidget(new JoystickAxis(centerX - 150, y, 60, 20, keybinds.get(15 + 4), null));
        //rt
        addWidget(new JoystickAxis(centerX + 90, y, 60, 20, keybinds.get(15 + 5), null));

        //ls axis
        y = centerY + 30;
        addWidget(new JoystickAxis(centerX - 90, y, 60, 60, keybinds.get(15), keybinds.get(15 + 1)));
        //rs axis
        addWidget(new JoystickAxis(centerX + 30, y, 60, 60, keybinds.get(15 + 2), keybinds.get(15 + 3)));

        //ls
        addWidget(new JoystickButton(centerX - 90, y, 60, 60, keybinds.get(9), true));
        //rs
        addWidget(new JoystickButton(centerX + 30, y, 60, 60, keybinds.get(10), true));
    }

    @Override
    protected void addBackButton() {
        //close button
        Button closeButton = new Button(width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(new Resource("textures/gui/icons/close.png"));
        closeButton.setTooltip(Text.translated("gui.close"));
        addWidget(closeButton);
    }

    @Override
    public void close() {
        for (Keybind keybind : keybinds)
            Keybind.unregister(keybind);
        keybinds.clear();
        super.close();
    }

    @Override
    public void tick() {
        super.tick();
        Keybind.flush();
    }

    @Override
    public boolean gamepadButtonPress(int button, boolean pressed, int joystick) {
        Keybind.gamepadButtonPress(button, pressed, joystick);
        return true;
        //return super.gamepadButtonPress(button, pressed, joystick);
    }

    @Override
    public boolean gamepadAxisMove(int axis, float value, int joystick, float lastValue) {
        Keybind.gamepadAxisMove(axis, value, joystick, lastValue);
        return true;
        //return super.gamepadAxisMove(axis, value, joystick, lastValue);
    }

    protected static class JoystickButton extends Widget {
        private final Keybind keybind;
        private final boolean round;

        public JoystickButton(int x, int y, int width, int height, Keybind keybind, boolean round) {
            super(x, y, width, height);
            this.keybind = keybind;
            this.round = round;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            int color = getSkin().getInt(keybind.isPressed() ? "accent_color" : "text_color");

            if (round) {
                VertexConsumer.MAIN.consume(GeometryHelper.arc(matrices, getCenterX(), getCenterY(), getWidth() / 2f, 0, 360, 2, 16, color));
            } else {
                UIHelper.outline(matrices, this, 2, color);
            }
        }
    }

    protected static class JoystickAxis extends Widget {
        private final Keybind key1, key2;

        public JoystickAxis(int x, int y, int width, int height, Keybind keybind1, Keybind keybind2) {
            super(x, y, width, height);
            this.key1 = keybind1;
            this.key2 = keybind2;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            int baseColor = getSkin().getInt("text_color");
            int activeColor = getSkin().getInt("accent_color");

            if (key2 == null) {
                UIHelper.outline(matrices, this, 2, baseColor);
                float value = key1.getAxisValue();
                int x0 = getX() + 3;
                int x1 = getX() + getWidth() - 3;
                float x = Math.lerp(x0, x1, (value + 1f) / 2f);
                VertexConsumer.MAIN.consume(GeometryHelper.circle(matrices, x, getCenterY(), 3f, 12, activeColor));
            } else {
                VertexConsumer.MAIN.consume(GeometryHelper.arc(matrices, getCenterX(), getCenterY(), getWidth() / 2f, 0, 360, 2, 16, baseColor));

                float xValue = key1.getAxisValue();
                float yValue = key2.getAxisValue();

                Vector2f vec = new Vector2f(xValue, yValue);
                if (vec.lengthSquared() > 1f)
                    vec.normalize();

                float r = (getWidth() / 2f) - 3f;
                float x = getCenterX() + vec.x * r;
                float y = getCenterY() + vec.y * r;

                VertexConsumer.MAIN.consume(GeometryHelper.circle(matrices, x, y, 3f, 12, activeColor));
            }
        }
    }
}
