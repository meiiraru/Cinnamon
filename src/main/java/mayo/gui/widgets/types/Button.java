package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.Widget;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class Button extends Widget implements GUIListener {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/button.png"));

    private final Label message;
    private final Runnable toRun;
    private Text tooltip;

    private boolean
            active = true,
            hovered = false;

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
        this.hovered = UIHelper.isMouseOver(this, mouseX, mouseY);
        if (this.hovered) {
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
        message.setPos(getX() + getWidth() / 2, getY() + (int) ((getHeight() - Client.getInstance().font.lineHeight) / 2));
        message.render(matrices, mouseX, mouseY, delta);
    }

    public int getState() {
        if (!this.active)
            return 0;
        else if (this.hovered)
            return 2;
        else
            return 1;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (hovered && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            toRun.run();
            return true;
        }
        return GUIListener.super.mousePress(button, action, mods);
    }

    public void setTooltip(Text tooltip) {
        this.tooltip = tooltip;
    }
}
