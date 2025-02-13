package cinnamon.utils;

import cinnamon.render.Font;
import cinnamon.text.Formatting;
import cinnamon.text.Style;
import cinnamon.text.Text;

import java.util.ArrayList;
import java.util.List;

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
        String string = text.asString();
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

    public static Text addEllipsis(Text text, int width) {
        if (getWidth(text) <= width)
            return text;

        int ellipsisWidth = getWidth(ELLIPSIS);

        Text clamped = text.getStyle().getGuiStyle().font.clampToWidth(text, width - ellipsisWidth);
        clamped.append(ELLIPSIS);

        return clamped;
    }

    public static Text parseColorFormatting(Text text) {
        Text result = Text.empty();

        text.visit((string, style) -> {
            String[] split = string.split(Formatting.FORMATTING_CHAR + "");
            Text newText = Text.of(split[0]).withStyle(style);

            if (split.length < 2) {
                result.append(newText);
                return;
            }

            for (int i = 1; i < split.length; i++) {
                String s = split[i];

                if (s.isEmpty())
                    continue;

                int sub = 1;
                Formatting formatting = Formatting.byCode(s.charAt(0));
                if (formatting != null) {
                    if (formatting == Formatting.HEX) {
                        sub = 7;
                        int color = ColorUtils.rgbToInt(ColorUtils.hexStringToRGB(s.substring(1, sub)));
                        style = style.color(color + 0xFF000000);
                    } else {
                        style = style.formatted(formatting);
                    }
                }

                newText.append(Text.of(s.substring(sub)).withStyle(style));
            }

            result.append(newText);
        }, Style.EMPTY);

        return result;
    }

    public static Text replaceAll(Text text, String regex, String replacement) {
        Text result = Text.empty();

        text.visit((string, style) -> {
            String[] split = string.split(regex, -1);
            Text newText = Text.of(split[0]).withStyle(style);

            for (int i = 1; i < split.length; i++) {
                newText.append(replacement);
                newText.append(split[i]);
            }

            result.append(newText);
        }, Style.EMPTY);

        return result;
    }

    public static Text parseSimpleMarkdown(Text text) {
        Text result = Text.empty();

        text.visit((string, style) -> {
            //~~  ~~ strikethrough
            //**  ** bold
            //__  __ underline
            //||  || obfuscated
            //*  * or _  _ italic

            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);

            }


        }, Style.EMPTY);

        return result;
    }

    public static int getWidth(Text text) {
        List<Text> split = split(text, "\n");
        float width = 0f;
        Font f = text.getStyle().getGuiStyle().font;
        for (Text t : split)
            width = Math.max(width, f.width(t));

        return (int) width;
    }

    public static int getHeight(Text text) {
        String[] split = text.asString().split("\n", -1);
        Font f = text.getStyle().getGuiStyle().font;
        return (int) (f.lineHeight * split.length + f.lineGap * (split.length - 1));
    }
}
