package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.SelectableWidget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.sound.SoundCategory;
import mayo.text.Text;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class Button extends SelectableWidget {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/button.png"));
    private static final Resource CLICK_SOUND = new Resource("sounds/pop.ogg");

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
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX(), getY(),
                getWidth(), getHeight(),
                getState() * 16f, 0f,
                16, 16,
                48, 16
        );
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Font f = Client.getInstance().font;
        int x = getCenterX();
        int y = getCenterY() - TextUtils.getHeight(message, f) / 2;
        f.render(VertexConsumer.FONT_FLAT, matrices, x, y, message, TextUtils.Alignment.CENTER);
    }

    public int getState() {
        if (!this.isActive())
            return 0;
        else if (this.isHovered())
            return 2;
        else
            return 1;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            press();
            return true;
        }
        return super.mousePress(button, action, mods);
    }

    public void press() {
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

    public void setMessage(Text message) {
        this.message = message;
    }

    public void setAction(Consumer<Button> action) {
        this.action = action;
    }
}
