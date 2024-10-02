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


    public static Toast addToast(Text text, Font font) {
        Toast toast = new Toast(text, font);
        TOASTS.add(toast);
        return toast;
    }

    public static void renderToasts(MatrixStack matrices, int width, int height, float delta) {
        if (TOASTS.isEmpty())
            return;

        matrices.push();
        matrices.translate(Math.round(width / 2f), 4f - 1f, 999f);

        Iterator<Toast> iterator = TOASTS.iterator();
        for (int i = 0; i < TOASTS_LIMIT && iterator.hasNext(); i++) {
            Toast toast = iterator.next();

            if (toast.addedTime < 0) {
                toast.addedTime = Client.getInstance().ticks;
                logToast(toast);
            }

            if (toast.render(matrices, width, height, delta))
                iterator.remove();
        }

        matrices.pop();
    }

    public static void clearAll() {
        TOASTS.clear();
    }

    public static void clear(ToastType type) {
        TOASTS.removeIf(toast -> toast.type == type);
    }

    private static void logToast(Toast toast) {
        String log = "[Toast] " + toast.text.asString();
        switch (toast.type) {
            case WARN -> LOGGER.warn(log);
            case ERROR -> LOGGER.error(log);
            default -> LOGGER.info(log);
        }
    }

    public enum ToastType {
        INFO(null),
        SUCCESS(Colors.LIME.rgba),
        WARN(Colors.YELLOW.rgba),
        ERROR(Colors.RED.rgba),
        WORLD(null);

        public final Integer color;

        ToastType(Integer color) {
            this.color = color;
        }
    }


    // -- the toast -- //


    private final Text text;
    private final Font font;
    private final int width, height;
    private long addedTime = -1;

    private int length;
    private ToastType type;
    private Integer color;

    protected Toast(Text text, Font font) {
        this.text = text;
        this.font = font;
        this.width = TextUtils.getWidth(text, font);
        this.height = TextUtils.getHeight(text, font);
        length(DEFAULT_LENGTH);
        type(ToastType.INFO);
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
        int pos = Math.round(-bgWidth / 2f);

        int color = this.color == null ? GUIStyle.accentColor : this.color;
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE,
                pos, 0f, bgWidth, bgHeight,
                16f, 0f,
                16, 16, 48, 16,
                color
        );
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE,
                pos, 0f, bgWidth, bgHeight,
                32f, 0f,
                16, 16, 48, 16,
                color
        );

        //render text
        font.render(VertexConsumer.FONT, matrices, 0f, PADDING / 2f, text, Alignment.CENTER);

        //return
        matrices.translate(0f, bgHeight, 0f);
        return false;
    }

    public Toast length(int length) {
        this.length = length + ANIM * 2;
        return this;
    }

    public Toast type(ToastType type) {
        this.type = type;
        if (type.color != null)
            color(type.color);

        return this;
    }

    public Toast color(Integer color) {
        this.color = color;
        return this;
    }
}
