package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.sound.SoundCategory;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class Button extends SelectableWidget {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/button.png");
    private static final Resource CLICK_SOUND = new Resource("sounds/ui/click.ogg");

    private boolean silent;
    private boolean runOnHold;
    protected boolean holding;

    protected Text message;
    protected Consumer<Button> action;

    public Button(int x, int y, int width, int height, Text message, Consumer<Button> action) {
        super(x, y, width, height);
        this.message = message;
        this.action = action;
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices, mouseX, mouseY, delta);
        if (message != null)
            renderText(matrices, mouseX, mouseY, delta);
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE,
                getX(), getY(),
                getWidth(), getHeight(),
                getState() * 16f, 0f,
                16, 16,
                48, 16
        );
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Text text = getFormattedMessage();
        Font f = Client.getInstance().font;
        int x = getCenterX();
        int y = getCenterY() - TextUtils.getHeight(text, f) / 2;
        f.render(VertexConsumer.FONT, matrices, x, y, text, Alignment.CENTER);
    }

    @Override
    public boolean isHoveredOrFocused() {
        return super.isHoveredOrFocused() || holding;
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (!isActive())
            return null;

        //test for left mouse button while hovered
        if (button == GLFW_MOUSE_BUTTON_1 && isHovered()) {
            //when pressed, set the flag to true, and if allowed, run the action
            if (action == GLFW_PRESS) {
                holding = true;
                if (runOnHold)
                    onRun();
                return this;
            //otherwise when released, only if we were holding, run the action
            } else if (holding && action == GLFW_RELEASE) {
                holding = false;
                onRun();
                return this;
            }
        }

        //everything failed, but we were holding and allowed to run on hold
        if (holding) {
            holding = false;
            if (runOnHold)
                onRun();
            return this;
        }

        //super
        return super.mousePress(button, action, mods);
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isActive())
            return null;

        if (isFocused() && (action == GLFW_PRESS || (action == GLFW_RELEASE && runOnHold))) {
            switch (key) {
                case GLFW_KEY_SPACE, GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                    onRun();
                    return this;
                }
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }

    public void onRun() {
        if (!isSilent())
            playClickSound();
        if (action != null)
            action.accept(this);
    }

    public void playClickSound() {
        Client.getInstance().soundManager.playSound(CLICK_SOUND, SoundCategory.GUI);
    }

    public Text getMessage() {
        return message;
    }

    public Text getFormattedMessage() {
        if (message == null)
            return null;

        if (getState() == 0)
            return Text.empty().withStyle(Style.EMPTY.color(Colors.DARK_GRAY)).append(message);

        return message;
    }

    public void setMessage(Text message) {
        this.message = message;
    }

    public void setAction(Consumer<Button> action) {
        this.action = action;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public void setRunOnHold(boolean bool) {
        this.runOnHold = bool;
    }

    public boolean isHolding() {
        return holding;
    }
}
