package mayo.gui.widgets.types;

import mayo.gui.widgets.Widget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.TextUtils;

public class Label extends Widget {

    private final Font font;
    private Text text;
    private TextUtils.Alignment alignment = TextUtils.Alignment.LEFT;

    public Label(Text text, Font font, int x, int y) {
        super(x, y, (int) font.width(text), (int) font.height(text));
        this.text = text;
        this.font = font;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        font.render(VertexConsumer.FONT, matrices.peek(), getX(), getY(), text, alignment);
    }

    public Label setText(Text text) {
        this.text = text;
        setWidth((int) font.width(text));
        setHeight((int) font.height(text));

        return this;
    }

    public Label setAlignment(TextUtils.Alignment alignment) {
        this.alignment = alignment;
        return this;
    }
}