package mayo.text;

public class Style {

    public final static Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null, null);

    private final Integer color;
    private final Integer backgroundColor;
    private final Integer shadowColor;
    private final Integer outlineColor;

    private final Boolean bold;
    private final Boolean italic;
    private final Boolean underlined;
    private final Boolean obfuscated;
    private final Boolean strikethrough;

    private final Boolean shadow;
    private final Boolean outlined;

    private Style(
            Integer color,
            Integer backgroundColor,
            Integer shadowColor,
            Integer outlineColor,
            Boolean bold,
            Boolean italic,
            Boolean underlined,
            Boolean obfuscated,
            Boolean strikethrough,
            Boolean shadow,
            Boolean outlined
    ) {
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.shadowColor = shadowColor;
        this.outlineColor = outlineColor;

        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.obfuscated = obfuscated;
        this.strikethrough = strikethrough;

        this.shadow = shadow;
        this.outlined = outlined;
    }

    public Style applyParent(Style p) {
        if (this == EMPTY)
            return p;
        if (p == EMPTY)
            return this;

        return new Style(
                color           != null ? color            : p.color,
                backgroundColor != null ? backgroundColor  : p.backgroundColor,
                shadowColor     != null ? shadowColor      : p.shadowColor,
                outlineColor    != null ? outlineColor     : p.outlineColor,
                bold            != null ? bold             : p.bold,
                italic          != null ? italic           : p.italic,
                underlined      != null ? underlined       : p.underlined,
                obfuscated      != null ? obfuscated       : p.obfuscated,
                strikethrough   != null ? strikethrough    : p.strikethrough,
                shadow          != null ? shadow           : p.shadow,
                outlined        != null ? outlined         : p.outlined
        );
    }

    public Style color(Integer color) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style backgroundColor(Integer backgroundColor) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style shadowColor(Integer shadowColor) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style outlineColor(Integer outlineColor) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style bold(Boolean bold) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style italic(Boolean italic) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style underlined(Boolean underlined) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style obfuscated(Boolean obfuscated) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style strikethrough(Boolean strikethrough) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style shadow(Boolean shadow) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Style outlined(Boolean outlined) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, shadow, outlined);
    }

    public Integer getColor() {
        return color;
    }

    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public Integer getShadowColor() {
        return shadowColor;
    }

    public Integer getOutlineColor() {
        return outlineColor;
    }

    public boolean isBold() {
        return bold == Boolean.TRUE;
    }

    public boolean isItalic() {
        return italic == Boolean.TRUE;
    }

    public boolean isUnderlined() {
        return underlined == Boolean.TRUE;
    }

    public boolean isObfuscated() {
        return obfuscated == Boolean.TRUE;
    }

    public boolean isStrikethrough() {
        return strikethrough == Boolean.TRUE;
    }

    public boolean hasShadow() {
        return shadow == Boolean.TRUE;
    }

    public boolean hasOutline() {
        return outlined == Boolean.TRUE;
    }
}