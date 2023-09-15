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
        matrices.push();
        matrices.translate(getX(), getY(), 0f);
        font.render(VertexConsumer.FONT, matrices.peek(), text, alignment);
        matrices.pop();
    }

    public void setText(Text text) {
        this.text = text;
        setWidth((int) font.width(text));
        setHeight((int) font.height(text));
    }

    public void setAlignment(TextUtils.Alignment alignment) {
        this.alignment = alignment;
    }
}
