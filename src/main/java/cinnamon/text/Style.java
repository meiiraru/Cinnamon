package cinnamon.text;

import cinnamon.gui.GUISkin;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;

import java.util.Objects;

public class Style {

    public final static Style EMPTY = new Style();

    // -- bitmask constants -- //

    //booleans
    public static final int BOLD          = 1;
    public static final int ITALIC        = 1 << 1;
    public static final int UNDERLINED    = 1 << 2;
    public static final int OBFUSCATED    = 1 << 3;
    public static final int STRIKETHROUGH = 1 << 4;
    public static final int BACKGROUND    = 1 << 5;
    public static final int SHADOW        = 1 << 6;
    public static final int OUTLINED      = 1 << 7;

    //colors and offsets
    public static final int COLOR_SET         = 1 << 8;
    public static final int BG_COLOR_SET      = 1 << 9;
    public static final int SHADOW_COLOR_SET  = 1 << 10;
    public static final int OUTLINE_COLOR_SET = 1 << 11;
    public static final int ITALIC_OFFSET_SET = 1 << 12;
    public static final int BOLD_OFFSET_SET   = 1 << 13;
    public static final int SHADOW_OFFSET_SET = 1 << 14;

    // -- fields -- //

    private int mask = 0, flags = 0;

    private int
            color,
            backgroundColor,
            shadowColor,
            outlineColor,

            italicOffset,
            boldOffset,
            shadowOffset;

    private Resource guiSkin;
    private ClickEvent clickEvent;
    private HoverEvent hoverEvent;

    private Style() {}

    private Style(Style o) {
        this.mask            = o.mask;
        this.flags           = o.flags;
        this.color           = o.color;
        this.backgroundColor = o.backgroundColor;
        this.shadowColor     = o.shadowColor;
        this.outlineColor    = o.outlineColor;
        this.italicOffset    = o.italicOffset;
        this.boldOffset      = o.boldOffset;
        this.shadowOffset    = o.shadowOffset;
        this.guiSkin         = o.guiSkin;
        this.clickEvent      = o.clickEvent;
        this.hoverEvent      = o.hoverEvent;
    }

    public Style applyParent(Style p) {
        if (this == EMPTY)
            return p == null ? this : p;
        if (p == null || p == EMPTY)
            return this;

        Style s = new Style(p);

        s.flags = (this.flags & this.mask) | (p.flags & ~this.mask);
        s.mask  = this.mask | p.mask;

        if ((this.mask & COLOR_SET)         != 0) s.color           = this.color;
        if ((this.mask & BG_COLOR_SET)      != 0) s.backgroundColor = this.backgroundColor;
        if ((this.mask & SHADOW_COLOR_SET)  != 0) s.shadowColor     = this.shadowColor;
        if ((this.mask & OUTLINE_COLOR_SET) != 0) s.outlineColor    = this.outlineColor;
        if ((this.mask & ITALIC_OFFSET_SET) != 0) s.italicOffset    = this.italicOffset;
        if ((this.mask & BOLD_OFFSET_SET)   != 0) s.boldOffset      = this.boldOffset;
        if ((this.mask & SHADOW_OFFSET_SET) != 0) s.shadowOffset    = this.shadowOffset;

        if (guiSkin    != null) s.guiSkin    = this.guiSkin;
        if (clickEvent != null) s.clickEvent = this.clickEvent;
        if (hoverEvent != null) s.hoverEvent = this.hoverEvent;

        return s;
    }

    // -- helpers -- //

    private void setBoolean(int bit, Boolean value) {
        if (value == null) {
            this.mask  &= ~bit;
            this.flags &= ~bit;
        } else {
            this.mask |= bit;
            if (value) this.flags |= bit;
            else       this.flags &= ~bit;
        }
    }

    private boolean getBoolean(int bit) {
        return (mask & bit) != 0 && (flags & bit) != 0;
    }

    private int setInt(int bit, Integer value) {
        if (value == null) {
            this.mask &= ~bit;
            return 0;
        } else {
            this.mask |= bit;
            return value;
        }
    }

    // -- setters -- //

    public Style color(Colors color) {
        return this.color(color.argb);
    }

    public Style color(Integer color) {
        Style s = new Style(this);
        s.color = s.setInt(COLOR_SET, color);
        return s;
    }

    public Style backgroundColor(Colors color) {
        return this.backgroundColor(color.argb);
    }

    public Style backgroundColor(Integer backgroundColor) {
        Style s = new Style(this);
        s.backgroundColor = s.setInt(BG_COLOR_SET, backgroundColor);
        return s;
    }

    public Style shadowColor(Colors color) {
        return this.shadowColor(color.argb);
    }

    public Style shadowColor(Integer shadowColor) {
        Style s = new Style(this);
        s.shadowColor = s.setInt(SHADOW_COLOR_SET, shadowColor);
        return s;
    }

    public Style outlineColor(Colors color) {
        return this.outlineColor(color.argb);
    }

    public Style outlineColor(Integer outlineColor) {
        Style s = new Style(this);
        s.outlineColor = s.setInt(OUTLINE_COLOR_SET, outlineColor);
        return s;
    }

    public Style italicOffset(Integer italicOffset) {
        Style s = new Style(this);
        s.italicOffset = s.setInt(ITALIC_OFFSET_SET, italicOffset);
        return s;
    }

    public Style boldOffset(Integer boldOffset) {
        Style s = new Style(this);
        s.boldOffset = s.setInt(BOLD_OFFSET_SET, boldOffset);
        return s;
    }

    public Style shadowOffset(Integer shadowOffset) {
        Style s = new Style(this);
        s.shadowOffset = s.setInt(SHADOW_OFFSET_SET, shadowOffset);
        return s;
    }

    public Style bold(Boolean bold) {
        Style s = new Style(this);
        s.setBoolean(BOLD, bold);
        return s;
    }

    public Style italic(Boolean italic) {
        Style s = new Style(this);
        s.setBoolean(ITALIC, italic);
        return s;
    }

    public Style underlined(Boolean underlined) {
        Style s = new Style(this);
        s.setBoolean(UNDERLINED, underlined);
        return s;
    }

    public Style obfuscated(Boolean obfuscated) {
        Style s = new Style(this);
        s.setBoolean(OBFUSCATED, obfuscated);
        return s;
    }

    public Style strikethrough(Boolean strikethrough) {
        Style s = new Style(this);
        s.setBoolean(STRIKETHROUGH, strikethrough);
        return s;
    }

    public Style background(Boolean background) {
        Style s = new Style(this);
        s.setBoolean(BACKGROUND, background);
        return s;
    }

    public Style shadow(Boolean shadow) {
        Style s = new Style(this);
        s.setBoolean(SHADOW, shadow);
        return s;
    }

    public Style outlined(Boolean outlined) {
        Style s = new Style(this);
        s.setBoolean(OUTLINED, outlined);
        return s;
    }

    public Style guiSkin(Resource skin) {
        Style s = new Style(this);
        s.guiSkin = skin;
        return s;
    }

    public Style clickEvent(ClickEvent event) {
        Style s = new Style(this);
        s.clickEvent = event;
        return s;
    }

    public Style hoverEvent(HoverEvent event) {
        Style s = new Style(this);
        s.hoverEvent = event;
        return s;
    }

    public Style formatted(Formatting... formats) {
        Style style = this;
        for (Formatting format : formats)
            style = format == Formatting.RESET ? format.style : format.style.applyParent(style);
        return style;
    }

    // -- getters -- //

    public int getColor() {
        return (mask & COLOR_SET) != 0 ? color : getGuiSkin().getInt("text_color");
    }

    public int getBackgroundColor() {
        return (mask & BG_COLOR_SET) != 0 ? backgroundColor : getGuiSkin().getInt("background_color");
    }

    public int getShadowColor() {
        return (mask & SHADOW_COLOR_SET) != 0 ? shadowColor : getGuiSkin().getInt("shadow_color");
    }

    public int getOutlineColor() {
        return (mask & OUTLINE_COLOR_SET) != 0 ? outlineColor : getGuiSkin().getInt("outline_color");
    }

    public int getItalicOffset() {
        return (mask & ITALIC_OFFSET_SET) != 0 ? italicOffset : getGuiSkin().getInt("italic_offset");
    }

    public int getBoldOffset() {
        return (mask & BOLD_OFFSET_SET) != 0 ? boldOffset : getGuiSkin().getInt("bold_offset");
    }

    public int getShadowOffset() {
        return (mask & SHADOW_OFFSET_SET) != 0 ? shadowOffset : getGuiSkin().getInt("shadow_offset");
    }

    public boolean isBold() {
        return getBoolean(BOLD);
    }

    public boolean isItalic() {
        return getBoolean(ITALIC);
    }

    public boolean isUnderlined() {
        return getBoolean(UNDERLINED);
    }

    public boolean isObfuscated() {
        return getBoolean(OBFUSCATED);
    }

    public boolean isStrikethrough() {
        return getBoolean(STRIKETHROUGH);
    }

    public boolean hasBackground() {
        return getBoolean(BACKGROUND);
    }

    public boolean hasShadow() {
        return getBoolean(SHADOW);
    }

    public boolean hasOutline() {
        return getBoolean(OUTLINED);
    }

    public GUISkin getGuiSkin() {
        return GUISkin.of(guiSkin == null ? GUISkin.getCurrentSkinRes() : guiSkin);
    }

    public ClickEvent getClickEvent() {
        return clickEvent;
    }

    public HoverEvent getHoverEvent() {
        return hoverEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Style style))
            return false;

        return  mask            == style.mask            &&
                flags           == style.flags           &&
                color           == style.color           &&
                backgroundColor == style.backgroundColor &&
                shadowColor     == style.shadowColor     &&
                outlineColor    == style.outlineColor    &&
                italicOffset    == style.italicOffset    &&
                boldOffset      == style.boldOffset      &&
                shadowOffset    == style.shadowOffset    &&
                Objects.equals(guiSkin,    style.guiSkin)    &&
                Objects.equals(clickEvent, style.clickEvent) &&
                Objects.equals(hoverEvent, style.hoverEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mask, flags,
                color, backgroundColor, shadowColor, outlineColor,
                italicOffset, boldOffset, shadowOffset,
                guiSkin, clickEvent, hoverEvent
        );
    }
}
