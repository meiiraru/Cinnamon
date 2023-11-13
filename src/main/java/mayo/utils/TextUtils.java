package mayo.utils;

import mayo.render.Font;
import mayo.text.Style;
import mayo.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class TextUtils {

    private static final Text ELLIPSIS = Text.of("...");

    public static List<Text> split(Text text, String regex) {
        List<Text> list = new ArrayList<>();
        Text[] currentText = {Text.empty()};

        text.visit((s, style) -> {
            String[] lines = s.split(regex, -1);

            for (int i = 0; i < lines.length; i++) {
                if (i != 0) {
                    list.add(currentText[0]);
                    currentText[0] = Text.empty();
                }

                currentText[0].append(Text.of(lines[i]).withStyle(style));
            }
        }, Style.EMPTY);

        list.add(currentText[0]);
        return list;
    }

    public static Text reverse(Text text) {
        Text[] builder = {Text.empty()};
        text.visit((string, style) -> {
            StringBuilder str = new StringBuilder(string).reverse();
            builder[0] = Text.of(str.toString()).withStyle(style).append(builder[0]);
        }, Style.EMPTY);
        return builder[0];
    }

    public static Text substring(Text text, int beginIndex, int endIndex) {
        StringBuilder counter = new StringBuilder();
        Text builder = Text.empty();
        text.visit((string, style) -> {
            int index = counter.length();
            int len = string.length();

            if (index <= endIndex && index + len >= beginIndex) {
                int sub = Math.max(beginIndex - index, 0);
                int top = Math.min(endIndex - index, len);
                builder.append(Text.of(string.substring(sub, top)).withStyle(style));
            }

            counter.append(string);
            return counter.length() > endIndex;
        }, Style.EMPTY);
        return builder;
    }

    public static Text trim(Text text) {
        String string = text.getText();
        int start = 0;
        int end = string.length();

        //trim
        while (start < end && string.charAt(start) <= ' ')
            start++;
        while (start < end && string.charAt(end - 1) <= ' ')
            end--;

        //apply trim
        return substring(text, start, end);
    }

    public static Text addEllipsis(Text text, Font font, int width) {
        if (getWidth(text, font) <= width)
            return text;

        int ellipsisWidth = getWidth(ELLIPSIS, font);

        Text clamped = font.clampToWidth(text, width - ellipsisWidth);
        clamped.append(ELLIPSIS);

        return clamped;
    }

    public enum Alignment {
        LEFT((font, text) -> 0f),
        RIGHT((font, text) -> -font.width(text)),
        CENTER((font, text) -> -font.width(text) / 2f);

        private final BiFunction<Font, Text, Float> textFunction;

        Alignment(BiFunction<Font, Text, Float> textFunction) {
            this.textFunction = textFunction;
        }

        public float apply(Font font, Text text) {
            return textFunction.apply(font, text);
        }
    }

    public static int getWidth(Text text, Font font) {
        List<Text> split = split(text, "\n");
        float width = 0f;
        for (Text t : split)
            width = Math.max(width, font.width(t));

        return (int) width;
    }

    public static int getHeight(Text text, Font font) {
        String[] split = text.asString().split("\n", -1);
        return (int) (font.lineHeight * split.length);
    }
}
