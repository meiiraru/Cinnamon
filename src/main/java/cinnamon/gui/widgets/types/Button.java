package cinnamon.gui.widgets.types;

import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundManager;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class Button extends SelectableWidget {

    private static final Resource CLICK_SOUND = new Resource("sounds/ui/click.ogg");

    private boolean silent;
    private boolean invisible;
    private boolean runOnHold;
    private boolean holding;

    protected Text message;
    protected Consumer<Button> action;
    protected Resource icon;

    public Button(int x, int y, int width, int height, Text message, Consumer<Button> action) {
        super(x, y, width, height);
        this.message = message;
        this.action = action;
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (invisible)
            return;

        if (icon == null)
            renderBackground(matrices, mouseX, mouseY, delta);
        else
            renderIcon(matrices, mouseX, mouseY, delta);
        if (message != null) {
            matrices.push();
            matrices.translate(0, 0, UIHelper.getDepthOffset());
            renderText(matrices, mouseX, mouseY, delta);
            matrices.pop();
        }
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, getStyle().buttonTex,
                getX(), getY(),
                getWidth(), getHeight(),
                getState() * 16f, 0f,
                16, 16,
                64, 16
        );
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Text text = getFormattedMessage();
        int x = getCenterX();
        int y = getCenterY() + (isHolding() ? getStyle().pressYOffset : 0);
        text.render(VertexConsumer.FONT, matrices, x, y, Alignment.CENTER);
    }

    protected void renderIcon(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int size = Math.min(getWidth(), getHeight());
        VertexConsumer.MAIN.consume(GeometryHelper.quad(
                matrices,
                getCenterX() - (int) (size / 2f), getCenterY() - (int) (size / 2f),
                size, size,
                getState(), 0f,
                1f, 1f,
                4, 1
        ), icon);
    }

    @Override
    public int getState() {
        if (!this.isActive())
            return 0;
        else if (this.isHolding())
            return 3;
        else if (this.isHoveredOrFocused())
            return 2;
        else
            return 1;
    }

    @Override
    public boolean isHoveredOrFocused() {
        return super.isHoveredOrFocused() || isHolding();
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (!isActive())
            return null;

        //test for left mouse button while hovered
        if (executeHold(button == GLFW_MOUSE_BUTTON_1 && isHovered(), action))
            return this;

        return super.mousePress(button, action, mods);
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isActive())
            return null;

        //test for space or enter buttons while focused
        if (executeHold((key == GLFW_KEY_SPACE || key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) && isFocused(), action))
            return this;

        return super.keyPress(key, scancode, action, mods);
    }

    private boolean executeHold(boolean check, int action) {
        //is passed the input test
        if (check) {
            //when pressed (or repeat), set the flag to true, and if allowed, run the action
            if (action == GLFW_PRESS) {
                holding = true;
                if (runOnHold)
                    onRun();
                return true;
                //otherwise when released, only if we were holding, run the action
            } else if (holding && action == GLFW_RELEASE) {
                holding = false;
                onRun();
                return true;
            }

            //wait when repeating
            if (action == GLFW_REPEAT)
                return true;
        }

        //everything failed, but we were holding and allowed to run on hold
        if (holding) {
            holding = false;
            if (runOnHold)
                onRun();
            return true;
        }

        return false;
    }

    public void onRun() {
        if (!isSilent())
            playClickSound();
        if (action != null)
            action.accept(this);
    }

    public void playClickSound() {
        SoundManager.playSound(CLICK_SOUND, SoundCategory.GUI);
    }

    public Text getMessage() {
        return message;
    }

    public Text getFormattedMessage() {
        if (message == null)
            return null;

        Style style = Style.EMPTY.guiStyle(getStyleRes());

        if (getState() == 0)
            style = style.color(style.getGuiStyle().disabledColor);

        return Text.empty().withStyle(style).append(message);
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

    public void setIcon(Resource icon) {
        this.icon = icon;
    }

    public Resource getIcon() {
        return icon;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }
}
