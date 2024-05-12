package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.SelectableWidget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.sound.SoundCategory;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.*;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class Button extends SelectableWidget {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/button.png");
    private static final Resource CLICK_SOUND = new Resource("sounds/pop.ogg");

    private boolean silent;
    protected boolean mouseSelected;

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
        return super.isHoveredOrFocused() || mouseSelected;
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (!isActive())
            return null;

        //test for left mouse button
        if (button == GLFW_MOUSE_BUTTON_1) {
            //test the click when hovered, otherwise cancel the click
            if (isHovered()) {
                //if the button was released while clicking, run the function
                if (mouseSelected && action == GLFW_RELEASE) {
                    onRun();
                    mouseSelected = false;
                    return this;
                }

                //update click based if the button was pressed
                mouseSelected = action == GLFW_PRESS;
            } else {
                mouseSelected = false;
            }
        }

        return super.mousePress(button, action, mods);
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isActive())
            return null;

        if (isFocused() && action == GLFW_PRESS) {
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

    public boolean isDragged() {
        return mouseSelected;
    }
}
