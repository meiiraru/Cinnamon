package mayo.gui;

import mayo.Client;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Meth;
import mayo.utils.Resource;
import mayo.utils.UIHelper;
import org.joml.Matrix4f;

import java.util.LinkedList;
import java.util.Queue;

public class Toast {

    // -- properties -- //

    protected static final int
            LENGTH = 100,
            ANIM = 5,
            PADDING = 12;
    private static final Texture TEXTURE = new Texture(new Resource("textures/gui/toast.png"));
    private static final Queue<Toast> TOASTS = new LinkedList<>();


    // -- toast functions -- //


    public static void renderToasts(MatrixStack matrices, int width, int height, float delta) {
        if (TOASTS.isEmpty())
            return;

        Toast toast = TOASTS.peek();
        if (toast.addedTime < 0)
            toast.addedTime = Client.getInstance().ticks;

        if (toast.render(matrices, width, height, delta))
            TOASTS.remove();
    }

    public static void addToast(Text text, Font font) {
        String str = text.asString();
        for (Toast toast : TOASTS) {
            //update toast if it already exists
            if (toast.text.asString().equals(str)) {
                toast.text = text;
                toast.font = font;
                toast.addedTime = Client.getInstance().ticks - ANIM;
                return;
            }
        }

        //otherwise queue a new toast
        TOASTS.add(new Toast(text, font));
    }

    public static void clearAll() {
        TOASTS.clear();
    }


    // -- the toast -- //


    private Text text;
    private Font font;
    private int addedTime = -1;

    protected Toast(Text text, Font font) {
        this.text = text;
        this.font = font;
    }

    protected boolean render(MatrixStack matrices, int width, int height, float delta) {
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
            y = Meth.lerp(-tHeight - PADDING, 4f, life / ANIM);
        } else if (life >= LENGTH - ANIM) {
            y = Meth.lerp(4f, -tHeight - PADDING, (life - (LENGTH - ANIM)) / ANIM);
        } else {
            y = 4f;
        }

        matrices.push();
        matrices.translate((width - tWidth - PADDING) / 2, y, 999f);
        Matrix4f matrix = matrices.peek();

        //render background
        UIHelper.nineQuad(VertexConsumer.GUI, matrix, TEXTURE.getID(), 0f, 0f, tWidth + PADDING, tHeight + PADDING);

        //render text
        font.render(VertexConsumer.FONT, matrices.peek(), PADDING / 2f, PADDING / 2f, text);

        matrices.pop();
        return false;
    }
}