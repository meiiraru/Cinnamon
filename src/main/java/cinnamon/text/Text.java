package cinnamon.text;

import cinnamon.lang.LangManager;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Alignment;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Text {

    private final List<Text> children = new ArrayList<>();
    private final String text;
    private final boolean translatable;
    private final Object[] translationArgs;
    private Style style;

    private Text(String text) {
        this(text, false, null, Style.EMPTY);
    }

    private Text(String text, boolean translatable, Object[] translationArgs, Style style) {
        this.text = text;
        this.translatable = translatable;
        this.translationArgs = translationArgs;
        this.style = style;
    }

    public static Text empty() {
        return of("");
    }

    public static Text of(Object text) {
        return new Text(String.valueOf(text));
    }

    public static Text translated(String translation, Object... args) {
        return new Text(translation, true, args, Style.EMPTY);
    }

    public Text copy() {
        Text t = new Text(text, translatable, translationArgs, style);
        for (Text child : children)
            t.children.add(child.copy());
        return t;
    }

    public Text withStyle(Text other) {
        return withStyle(other.getStyle());
    }

    public Text withStyle(Style style) {
        this.style = style.applyParent(this.style);
        return this;
    }

    public Text append(Text text) {
        this.children.add(text);
        return this;
    }

    public Text append(String text) {
        return this.append(Text.of(text));
    }

    public Text appendTranslated(String text, Object... args) {
        return this.append(Text.translated(text, args));
    }

    public String getRawText() {
        return text;
    }

    public String getTranslatedText() {
        return !translatable ? text : LangManager.get(text, translationArgs);
    }

    public String asString() {
        StringBuilder sb = new StringBuilder(getTranslatedText());
        for (Text child : children)
            sb.append(child.asString());
        return sb.toString();
    }

    public Style getStyle() {
        return style;
    }

    public List<Text> getChildren() {
        return children;
    }

    public void visit(BiConsumer<String, Style> consumer, Style initialStyle) {
        Style s = style.applyParent(initialStyle);
        consumer.accept(getTranslatedText(), s);
        for (Text child : children)
            child.visit(consumer, s);
    }

    public boolean visit(BiFunction<String, Style, Boolean> function, Style initialStyle) {
        Style s = style.applyParent(initialStyle);

        Boolean bool = function.apply(getTranslatedText(), s);
        if (bool != null && bool)
            return true;

        for (Text child : children)
            if (child.visit(function, s))
                return true;

        return false;
    }

    public void render(VertexConsumer consumer, MatrixStack matrices, float x, float y) {
        render(consumer, matrices, x, y, Alignment.TOP_LEFT);
    }

    public void render(VertexConsumer consumer, MatrixStack matrices, float x, float y, Alignment alignment) {
        render(consumer, matrices, x, y, alignment, 1);
    }

    public void render(VertexConsumer consumer, MatrixStack matrices, float x, float y, Alignment alignment, int indexScaling) {
        List<Text> list = TextUtils.split(this, "\n");
        Font font = style.getGuiStyle().font;

        int size = list.size();
        float yOffset = alignment.getHeightOffset(font.lineHeight * size + font.lineGap * (size - 1));

        for (int i = 0; i < size; i++) {
            Text t = list.get(i);
            int x2 = Math.round(alignment.getWidthOffset(font.width(t)));
            int y2 = Math.round(font.lineHeight * (i + 1) + font.descent + font.lineGap * i + yOffset);
            font.bake(consumer, matrices, t, x + x2, y + y2, indexScaling * UIHelper.getDepthOffset());
        }
    }
}
