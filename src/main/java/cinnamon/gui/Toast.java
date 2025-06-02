package cinnamon.gui;

import cinnamon.Client;
import cinnamon.logger.Logger;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Toast {

    private static final Logger LOGGER = new Logger("toast");

    // -- properties -- //

    public static final int DEFAULT_LENGTH = 3 * Client.TPS;
    protected static final int
            ANIM = 5,
            PADDING = 6,
            TOASTS_LIMIT = 5;
    private static final List<Toast> TOASTS = new ArrayList<>();


    // -- toast functions -- //


    public static Toast addToast(Text text) {
        Toast toast = new Toast(text);
        TOASTS.add(toast);
        return toast;
    }

    public static void renderToasts(MatrixStack matrices, int width, int height, float delta) {
        if (TOASTS.isEmpty())
            return;

        matrices.pushMatrix();
        matrices.translate(Math.round(width / 2f), 4f - 1f, 100f);

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

        matrices.popMatrix();
    }

    public static void clearAll() {
        TOASTS.clear();
    }

    public static void clear(ToastType type) {
        TOASTS.removeIf(toast -> toast.type == type);
    }

    private static void logToast(Toast toast) {
        String log = toast.text.asString();
        if (log.contains("\n"))
            log = "\n" + log;

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
    private int width, height;
    private long addedTime = -1;

    private int length;
    private ToastType type;
    private Integer color;
    private Resource style;

    protected Toast(Text text) {
        this.text = text;
        length(DEFAULT_LENGTH);
        type(ToastType.INFO);
        style(GUIStyle.DEFAULT_STYLE);
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

        GUIStyle style = GUIStyle.of(this.style);
        int color = this.color == null ? style.accentColor : this.color;
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, style.toastTex,
                pos, 0f, bgWidth, bgHeight,
                16f, 0f,
                16, 16, 48, 16,
                color
        );
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, style.toastTex,
                pos, 0f, bgWidth, bgHeight,
                32f, 0f,
                16, 16, 48, 16,
                color
        );

        //render text
        float d = UIHelper.getDepthOffset();
        matrices.translate(0f, 0f, d);
        Text.empty().withStyle(Style.EMPTY.guiStyle(this.style)).append(text).render(VertexConsumer.FONT, matrices, 0f, PADDING / 2f, Alignment.TOP_CENTER);

        //return
        matrices.translate(0f, bgHeight, -d);
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

    public Toast style(Resource style) {
        this.style = style;
        Text t = Text.empty().withStyle(Style.EMPTY.guiStyle(style)).append(text);
        this.width = TextUtils.getWidth(t);
        this.height = TextUtils.getHeight(t);
        return this;
    }
}
