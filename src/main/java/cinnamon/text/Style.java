package cinnamon.text;

import cinnamon.gui.GUIStyle;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;

public class Style {

    public final static Style EMPTY = new Style();

    private Integer
            color,
            backgroundColor,
            shadowColor,
            outlineColor,

            italicOffset,
            boldOffset,
            shadowOffset;

    private Boolean
            bold,
            italic,
            underlined,
            obfuscated,
            strikethrough,

            background,
            shadow,
            outlined;

    private Resource
            guiStyle;

    private Style() {}

    private Style(Style o) {
        this.color           = o.color;
        this.backgroundColor = o.backgroundColor;
        this.shadowColor     = o.shadowColor;
        this.outlineColor    = o.outlineColor;
        this.italicOffset    = o.italicOffset;
        this.boldOffset      = o.boldOffset;
        this.shadowOffset    = o.shadowOffset;
        this.bold            = o.bold;
        this.italic          = o.italic;
        this.underlined      = o.underlined;
        this.obfuscated      = o.obfuscated;
        this.strikethrough   = o.strikethrough;
        this.background      = o.background;
        this.shadow          = o.shadow;
        this.outlined        = o.outlined;
        this.guiStyle        = o.guiStyle;
    }

    public Style applyParent(Style p) {
        if (this == EMPTY)
            return p == null ? this : p;
        if (p == null || p == EMPTY)
            return this;

        Style s = new Style(p);
        if (color           != null) s.color           = color;
        if (backgroundColor != null) s.backgroundColor = backgroundColor;
        if (shadowColor     != null) s.shadowColor     = shadowColor;
        if (outlineColor    != null) s.outlineColor    = outlineColor;
        if (italicOffset    != null) s.italicOffset    = italicOffset;
        if (boldOffset      != null) s.boldOffset      = boldOffset;
        if (shadowOffset    != null) s.shadowOffset    = shadowOffset;
        if (bold            != null) s.bold            = bold;
        if (italic          != null) s.italic          = italic;
        if (underlined      != null) s.underlined      = underlined;
        if (obfuscated      != null) s.obfuscated      = obfuscated;
        if (strikethrough   != null) s.strikethrough   = strikethrough;
        if (background      != null) s.background      = background;
        if (shadow          != null) s.shadow          = shadow;
        if (outlined        != null) s.outlined        = outlined;
        if (guiStyle        != null) s.guiStyle        = guiStyle;
        return s;
    }

    public Style color(Colors color) {
        return this.color(color.rgba);
    }

    public Style color(Integer color) {
        Style s = new Style(this);
        s.color = color;
        return s;
    }

    public Style backgroundColor(Colors color) {
        return this.backgroundColor(color.rgba);
    }

    public Style backgroundColor(Integer backgroundColor) {
        Style s = new Style(this);
        s.backgroundColor = backgroundColor;
        return s;
    }

    public Style shadowColor(Colors color) {
        return this.shadowColor(color.rgba);
    }

    public Style shadowColor(Integer shadowColor) {
        Style s = new Style(this);
        s.shadowColor = shadowColor;
        return s;
    }

    public Style outlineColor(Colors color) {
        return this.outlineColor(color.rgba);
    }

    public Style outlineColor(Integer outlineColor) {
        Style s = new Style(this);
        s.outlineColor = outlineColor;
        return s;
    }

    public Style italicOffset(Integer italicOffset) {
        Style s = new Style(this);
        s.italicOffset = italicOffset;
        return s;
    }

    public Style boldOffset(Integer boldOffset) {
        Style s = new Style(this);
        s.boldOffset = boldOffset;
        return s;
    }

    public Style shadowOffset(Integer shadowOffset) {
        Style s = new Style(this);
        s.shadowOffset = shadowOffset;
        return s;
    }

    public Style bold(Boolean bold) {
        Style s = new Style(this);
        s.bold = bold;
        return s;
    }

    public Style italic(Boolean italic) {
        Style s = new Style(this);
        s.italic = italic;
        return s;
    }

    public Style underlined(Boolean underlined) {
        Style s = new Style(this);
        s.underlined = underlined;
        return s;
    }

    public Style obfuscated(Boolean obfuscated) {
        Style s = new Style(this);
        s.obfuscated = obfuscated;
        return s;
    }

    public Style strikethrough(Boolean strikethrough) {
        Style s = new Style(this);
        s.strikethrough = strikethrough;
        return s;
    }

    public Style background(Boolean background) {
        Style s = new Style(this);
        s.background = background;
        return s;
    }

    public Style shadow(Boolean shadow) {
        Style s = new Style(this);
        s.shadow = shadow;
        return s;
    }

    public Style outlined(Boolean outlined) {
        Style s = new Style(this);
        s.outlined = outlined;
        return s;
    }

    public Style guiStyle(Resource guiStyle) {
        Style s = new Style(this);
        s.guiStyle = guiStyle;
        return s;
    }

    public Style formatted(Formatting... formats) {
        Style style = this;
        for (Formatting format : formats)
            style = format == Formatting.RESET ? format.style : format.style.applyParent(style);
        return style;
    }

    public int getColor() {
        return color == null ? getGuiStyle().getInt("text_color") : color;
    }

    public int getBackgroundColor() {
        return backgroundColor == null ? getGuiStyle().getInt("background_color") : backgroundColor;
    }

    public int getShadowColor() {
        return shadowColor == null ? getGuiStyle().getInt("shadow_color") : shadowColor;
    }

    public int getOutlineColor() {
        return outlineColor == null ? getGuiStyle().getInt("shadow_color") : outlineColor;
    }

    public int getItalicOffset() {
        return italicOffset == null ? getGuiStyle().getInt("italic_offset") : italicOffset;
    }

    public int getBoldOffset() {
        return boldOffset == null ? getGuiStyle().getInt("bold_offset") : boldOffset;
    }

    public int getShadowOffset() {
        return shadowOffset == null ? getGuiStyle().getInt("shadow_offset") : shadowOffset;
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

    public GUIStyle getGuiStyle() {
        return guiStyle == null ? GUIStyle.getDefault() : GUIStyle.of(guiStyle);
    }
}
