package cinnamon.text;

public enum Formatting {
    BLACK('0', 0x0C0C0C),
    DARK_BLUE('1', 0x0037DA),
    DARK_GREEN('2', 0x39B54A),
    DARK_CYAN('3', 0x008080),
    DARK_RED('4', 0x800000),
    DARK_PURPLE('5', 0x881798),
    ORANGE('6', 0xC19C00),
    LIGHT_GRAY('7', 0xC0C0C0),
    GRAY('8', 0x767676),
    BLUE('9', 0x3A96DD),
    GREEN('a', 0x55FF55),
    CYAN('b', 0x61D6D6),
    RED('c', 0xE74856),
    PINK('d', 0xFF72AD),
    YELLOW('e', 0xF9F1A5),
    WHITE('f', 0xF2F2F2),

    ITALIC('i', Style.EMPTY.italic(true)),
    UNDERLINE('u', Style.EMPTY.underlined(true)),
    STRIKETHROUGH('s', Style.EMPTY.strikethrough(true)),
    OBFUSCATED('o', Style.EMPTY.obfuscated(true)),
    BOLD('n', Style.EMPTY.bold(true)),

    RESET('r', Style.EMPTY);

    public static final char FORMATTING_CHAR = '&';

    public final char code;
    public final Style style;

    Formatting(char code, Style style) {
        this.code = code;
        this.style = style;
    }

    Formatting(char code, int rgb) {
        this(code, Style.EMPTY.color(rgb | 0xFF000000));
    }

    public static Formatting byCode(char code) {
        for (Formatting formatting : values())
            if (formatting.code == code)
                return formatting;
        return null;
    }
}
