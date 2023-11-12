package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.widgets.SelectableWidget;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.sound.SoundCategory;
import mayo.text.Text;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class Button extends SelectableWidget {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/button.png"));
    private static final Resource CLICK_SOUND = new Resource("sounds/pop.ogg");

    private final Label message;
    private final Runnable toRun;
    private Text tooltip;

    public Button(int x, int y, int width, int height, Text message, Runnable toRun) {
        super(x, y, width, height);

        if (message != null) {
            this.message = new Label(message, Client.getInstance().font, 0, 0);
            this.message.setAlignment(TextUtils.Alignment.CENTER);
        } else {
            this.message = null;
        }

        this.toRun = toRun;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.isHovered()) {
            Screen s = Client.getInstance().screen;
            if (s != null)
                s.tooltip = tooltip;
        }

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
        message.setPos(getCenterX(), getY() + (int) ((getHeight() - Client.getInstance().font.lineHeight) / 2));
        message.render(matrices, mouseX, mouseY, delta);
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
        toRun.run();
    }

    public void setTooltip(Text tooltip) {
        this.tooltip = tooltip;
    }

    public void playClickSound() {
        Client.getInstance().soundManager.playSound(CLICK_SOUND, SoundCategory.GUI);
    }
}
