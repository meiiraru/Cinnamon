package mayo.text;

public class Style {

    public final static Style EMPTY = new Style();

    public Integer color;
    public Integer backgroundColor;
    public Integer shadowColor;
    public Integer outlineColor;

    public Boolean bold;
    public Boolean italic;
    public Boolean underlined;
    public Boolean obfuscated;
    public Boolean strikethrough;

    public Boolean shadow;
    public Boolean outlined;

    public Style color(Integer i) {
        this.color = i;
        return this;
    }

    public Style backgroundColor(Integer i) {
        this.backgroundColor = i;
        return this;
    }

    public Style shadowColor(Integer i) {
        this.shadowColor = i;
        return this;
    }

    public Style outlineColor(Integer i) {
        this.outlineColor = i;
        return this;
    }

    public Style bold(Boolean bool) {
        this.bold = bool;
        return this;
    }

    public Style italic(Boolean bool) {
        this.italic = bool;
        return this;
    }

    public Style underlined(Boolean bool) {
        this.underlined = bool;
        return this;
    }

    public Style obfuscated(Boolean bool) {
        this.obfuscated = bool;
        return this;
    }

    public Style strikethrough(Boolean bool) {
        this.strikethrough = bool;
        return this;
    }

    public Style shadow(Boolean bool) {
        this.shadow = bool;
        return this;
    }

    public Style outlined(Boolean bool) {
        this.outlined = bool;
        return this;
    }
}
