package mayo.text;

import mayo.utils.Colors;

public class Style {

    public final static Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null, null, null);

    private final Integer
            color,
            backgroundColor,
            shadowColor,
            outlineColor;

    private final Boolean
            bold,
            italic,
            underlined,
            obfuscated,
            strikethrough;

    private final Boolean
            background,
            shadow,
            outlined;

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
            Boolean background,
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

        this.background = background;
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
                background      != null ? background       : p.background,
                shadow          != null ? shadow           : p.shadow,
                outlined        != null ? outlined         : p.outlined
        );
    }

    public Style color(Colors color) {
        return this.color(color.rgba);
    }

    public Style color(Integer color) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style backgroundColor(Colors color) {
        return this.backgroundColor(color.rgba);
    }

    public Style backgroundColor(Integer backgroundColor) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style shadowColor(Colors color) {
        return this.shadowColor(color.rgba);
    }

    public Style shadowColor(Integer shadowColor) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style outlineColor(Colors color) {
        return this.outlineColor(color.rgba);
    }

    public Style outlineColor(Integer outlineColor) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style bold(Boolean bold) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style italic(Boolean italic) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style underlined(Boolean underlined) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style obfuscated(Boolean obfuscated) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style strikethrough(Boolean strikethrough) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style background(Boolean background) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style shadow(Boolean shadow) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style outlined(Boolean outlined) {
        return new Style(color, backgroundColor, shadowColor, outlineColor, bold, italic, underlined, obfuscated, strikethrough, background, shadow, outlined);
    }

    public Style formatted(Formatting... formats) {
        Style style = this;
        for (Formatting format : formats)
            style = format == Formatting.RESET ? EMPTY: format.style.applyParent(style);
        return style;
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

    public boolean hasBackground() {
        return background == Boolean.TRUE;
    }

    public boolean hasShadow() {
        return shadow == Boolean.TRUE;
    }

    public boolean hasOutline() {
        return outlined == Boolean.TRUE;
    }
}
