package mayo.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Text {

    private final List<Text> children = new ArrayList<>();
    private final String text;
    private Style style;

    public Text(String text) {
        this(text, Style.EMPTY);
    }

    public Text(String text, Style style) {
        this.text = text;
        this.style = style;
    }

    public static Text empty() {
        return of("");
    }

    public static Text of(Object text) {
        return new Text(String.valueOf(text));
    }

    public Text copy() {
        Text t = new Text(text, style);
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

    public Text append(String text) {
        return this.append(Text.of(text));
    }

    public Text append(Text text) {
        this.children.add(text);
        return this;
    }

    public String getText() {
        return text;
    }

    public String asString() {
        StringBuilder sb = new StringBuilder(text);
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

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public void visit(BiConsumer<String, Style> consumer, Style initialStyle) {
        Style s = style.applyParent(initialStyle);
        consumer.accept(text, s);
        for (Text child : children)
            child.visit(consumer, s);
    }

    public boolean visit(BiFunction<String, Style, Boolean> function, Style initialStyle) {
        Style s = style.applyParent(initialStyle);

        Boolean bool = function.apply(text, s);
        if (bool != null && bool)
            return true;

        for (Text child : children)
            if (child.visit(function, s))
                return true;

        return false;
    }
}
