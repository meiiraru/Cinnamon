package cinnamon.gui;

import cinnamon.Client;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static cinnamon.Client.LOGGER;

public class Toast {

    // -- properties -- //

    public static final int DEFAULT_LENGTH = 60;
    protected static final int
            ANIM = 5,
            PADDING = 6,
            TOASTS_LIMIT = 5;
    private static final Resource TEXTURE = new Resource("textures/gui/widgets/toast.png");
    private static final List<Toast> TOASTS = new ArrayList<>();


    // -- toast functions -- //


    public static void renderToasts(MatrixStack matrices, int width, int height, float delta) {
        if (TOASTS.isEmpty())
            return;

        matrices.push();
        matrices.translate(Math.round(width / 2f), 4f - 1f, 999f);

        Iterator<Toast> iterator = TOASTS.iterator();
        for (int i = 0; i < TOASTS_LIMIT && iterator.hasNext(); i++) {
            Toast toast = iterator.next();

            if (toast.addedTime < 0)
                toast.addedTime = Client.getInstance().ticks;

            if (toast.render(matrices, width, height, delta))
                iterator.remove();
        }

        matrices.pop();
    }

    public static void addToast(Text text, Font font) {
        addToast(text, font, DEFAULT_LENGTH);
    }

    public static void addToast(Text text, Font font, int length) {
        addToast(text, font, length, ToastType.DEFAULT);
    }

    public static void addToast(Text text, Font font, ToastType type) {
        addToast(text, font, DEFAULT_LENGTH, type);
    }

    public static void addToast(Text text, Font font, int length, ToastType type) {
        //add a new toast
        TOASTS.add(new Toast(text, font, length, type));

        //logging
        String log = "[Toast] " + text.asString();
        switch (type) {
            case WARN -> LOGGER.warn(log);
            case ERROR -> LOGGER.error(log);
            case DEFAULT -> LOGGER.info(log);
        }
    }

    public static void clearAll() {
        TOASTS.clear();
    }

    public static void clear(ToastType type) {
        TOASTS.removeIf(toast -> toast.type == type);
    }

    public enum ToastType {
        DEFAULT,
        WARN(1),
        ERROR(2),
        WORLD;

        public final int textureIndex;

        ToastType() {
            this(0);
        }

        ToastType(int textureIndex) {
            this.textureIndex = textureIndex;
        }
    }


    // -- the toast -- //


    private final int length;
    private final ToastType type;
    private final Text text;
    private final Font font;
    private final int width, height;
    private long addedTime = -1;

    protected Toast(Text text, Font font, int length, ToastType type) {
        this.length = length + ANIM * 2;
        this.type = type;
        this.text = text;
        this.font = font;
        this.width = TextUtils.getWidth(text, font);
        this.height = TextUtils.getHeight(text, font);
    }

    protected boolean render(MatrixStack matrices, int width, int height, float delta) {
        //calculate life
        float life = Client.getInstance().ticks - addedTime + delta;
        if (life > length)
            return true;

        //set y animation offset
        float y;
        if (life <= ANIM) {
            y = Maths.lerp(-this.height - PADDING, 1f, life / ANIM);
        } else if (life >= length - ANIM) {
            y = Maths.lerp(1f, -this.height - PADDING, (life - (length - ANIM)) / ANIM);
        } else {
            y = 1f;
        }

        matrices.translate(0f, y, 0f);

        //render background
        int bgWidth = this.width + PADDING;
        int bgHeight = this.height + PADDING;

        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE,
                Math.round(-bgWidth / 2f), 0f,
                bgWidth, bgHeight,
                type.textureIndex * 16f, 0f,
                16, 16,
                48, 16
        );

        //render text
        font.render(VertexConsumer.FONT, matrices, 0f, PADDING / 2f, text, Alignment.CENTER);

        //return
        matrices.translate(0f, bgHeight, 0f);
        return false;
    }
}
