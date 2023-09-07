package mayo.text;

import java.util.ArrayList;
import java.util.List;

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

    public static Text of(String text) {
        return new Text(text);
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
}
