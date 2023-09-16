package mayo.gui;

import mayo.Client;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Meth;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.LinkedList;
import java.util.Queue;

public class Toast {

    private static final int
            LENGTH = 100,
            ANIM = 5;
    private static final Texture TEXTURE = new Texture(new Resource("textures/gui/toast.png"));
    private static final Queue<Toast> TOASTS = new LinkedList<>();

    public static void render(MatrixStack matrices, int width, int height, float delta) {
        if (TOASTS.isEmpty())
            return;

        Toast toast = TOASTS.peek();
        if (toast.addedTime < 0)
            toast.addedTime = Client.getInstance().ticks;

        if (toast._render(matrices, width, height, delta))
            TOASTS.remove();
    }

    public static void addToast(Text text, Font font) {
        TOASTS.add(new Toast(text, font));
    }


    // -- the toast -- //


    private final Text text;
    private final Font font;
    private int addedTime = -1;

    private Toast(Text text, Font font) {
        this.text = text;
        this.font = font;
    }

    private boolean _render(MatrixStack matrices, int width, int height, float delta) {
        //calculate life
        float life = Client.getInstance().ticks - addedTime + delta;
        if (life > LENGTH)
            return true;

        //grab text dimensions
        float tWidth = font.width(text);
        float tHeight = font.height(text);

        //set y animation offset
        float y;
        if (life <= ANIM) {
            y = Meth.lerp(-tHeight - 4f, 4f, life / ANIM);
        } else if (life >= LENGTH - ANIM) {
            y = Meth.lerp(4f, -tHeight - 4f, (life - (LENGTH - ANIM)) / ANIM);
        } else {
            y = 4f;
        }

        //render background
        UIHelper.nineQuad(VertexConsumer.GUI, TEXTURE.getID(), (width - tWidth) / 2 - 2, y, tWidth + 4, tHeight + 4);

        //render text
        matrices.push();
        matrices.translate(width / 2f, y + 2f, 0f);
        font.render(VertexConsumer.FONT, matrices.peek(), text, TextUtils.Alignment.CENTER);
        matrices.pop();

        return false;
    }
}
