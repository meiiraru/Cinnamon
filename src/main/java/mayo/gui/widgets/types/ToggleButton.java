package mayo.gui.widgets.types;

import mayo.Client;
import mayo.model.GeometryHelper;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Resource;
import mayo.utils.TextUtils;

import java.util.function.Consumer;

public class ToggleButton extends Button {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/toggle_box.png"));

    protected boolean toggled;

    public ToggleButton(int x, int y, int height, Text message) {
        super(x, y, 0, height, message, null);

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
                ), TEXTURE.getID()
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
        this.setWidth(8 + 2 + TextUtils.getWidth(message, Client.getInstance().font));
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
}
