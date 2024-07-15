package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;

import java.util.function.Consumer;

public class ToggleButton extends Button {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/toggle_box.png");

    protected boolean toggled;

    public ToggleButton(int x, int y, Text message) {
        super(x, y, 0, 0, message, null);

        //force updates
        setAction(null);
        setMessage(message);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        VertexConsumer.GUI.consume(
                GeometryHelper.quad(
                        matrices,
                        getX(), getCenterY() - 4,
                        8, 8,
                        getState() * 8f, toggled ? 8f : 0f,
                        8, 8,
                        24, 16
                ), TEXTURE
        );
    }

    @Override
    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Text text = getFormattedMessage();
        Font f = Client.getInstance().font;
        int y = getCenterY() - TextUtils.getHeight(text, f) / 2;
        f.render(VertexConsumer.FONT, matrices, getX() + 8 + 2, y, text);
    }

    @Override
    public void setAction(Consumer<Button> action) {
        Consumer<Button> consumer = button -> {
            toggled = !toggled;
            if (action != null)
                action.accept(this);
        };

        super.setAction(consumer);
    }

    @Override
    public void setMessage(Text message) {
        super.setMessage(message);

        Font f = Client.getInstance().font;
        this.setDimensions(
                Math.max(8, 8 + 2 + TextUtils.getWidth(message, f)),
                Math.max(8, TextUtils.getHeight(message, f))
        );
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
}
