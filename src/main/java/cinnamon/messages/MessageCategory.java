package cinnamon.messages;

import cinnamon.text.Formatting;
import cinnamon.text.Style;
import cinnamon.utils.Colors;

import java.util.function.Function;

import static cinnamon.messages.MessageManager.DEFAULT_STYLE;

public enum MessageCategory {
    CHAT(s -> s),
    WHISPER(s -> s.formatted(Formatting.PINK, Formatting.ITALIC)),
    PARTY(s -> s.formatted(Formatting.CYAN)),
    SYSTEM(s -> s.formatted(Formatting.LIGHT_GRAY, Formatting.ITALIC)),
    SERVER(s -> s.formatted(Formatting.YELLOW)),
    ANNOUNCEMENT(s -> s.shadow(false).outlined(true).outlineColor(Colors.BLUE)),
    WORLD(s -> s.formatted(Formatting.GREEN));

    private final Style style;

    MessageCategory(Function<Style, Style> style) {
        this.style = style.apply(DEFAULT_STYLE);
    }

    public Style getStyle() {
        return style;
    }
}
