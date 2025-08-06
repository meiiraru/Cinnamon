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

    public static Text addEllipsis(Text text, float width) {
        if (getWidth(text) <= width)
            return text;

        int ellipsisWidth = getWidth(ELLIPSIS);

        Text clamped = clampToWidth(text, width - ellipsisWidth);
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

    public static Text clampToWidth(Text text, float width) {
        return clampToWidth(text, width, false);
    }

    public static Text clampToWidth(Text text, float width, boolean roundToClosest) {
        //prepare vars
        Text builder = Text.empty();
        boolean[] prevItalic = {false};
        float[] x = {0f, 0f};

        //iterate text
        text.visit((s, style) -> {
            boolean bold = style.isBold();
            boolean italic = style.isItalic();

            //italic
            if (!prevItalic[0] && italic)
                x[0] += style.getItalicOffset();
            prevItalic[0] = italic;

            //text allowed to add
            StringBuilder current = new StringBuilder();
            boolean stop = false;
            Font f = style.getGuiStyle().getFont();

            //iterate over the text
            for (int i = 0; i < s.length(); ) {
                //char
                int c = s.codePointAt(i);
                i += Character.charCount(c);
                x[0] += f.width(c);

                //kerning
                if (i < s.length())
                    x[0] += f.getKerning(c, s.codePointAt(i));

                //bold special
                if (bold)
                    x[0] += style.getBoldOffset();

                //check width
                if (x[0] <= width) {
                    current.appendCodePoint(c);
                } else {
                    if (roundToClosest && x[0] - width < width - x[1])
                        current.appendCodePoint(c);
                    stop = true;
                    break;
                }

                x[1] = x[0];
            }

            //append allowed text
            builder.append(Text.of(current.toString()).withStyle(style));
            return stop;
        }, Style.EMPTY);

        //return
        return builder;
    }

    public static List<Text> warpToWidth(Text text, float width) {
        List<Text> list = new ArrayList<>();
        Text toVisit = Text.empty().append(text).append(" ");

        //[0] word buffer, [1] line buffer
        Text[] textBuffer = {Text.empty(), Text.empty()};
        float[] widthBuffer = {0f, 0f};

        //iterate text
        toVisit.visit((s, style) -> {
            Font f = style.getGuiStyle().getFont();

            String[] words = s.split("((?<= )|(?= ))");
            for (String word : words) {
                Text t = Text.of(word).withStyle(style);
                float w = f.width(t);

                //just append when not a space
                if (!word.equals(" ")) {
                    //append text to the current word
                    textBuffer[0].append(t);
                    widthBuffer[0] += w;

                    //finish iteration
                    continue;
                }

                //feed word to the line at spaces

                //if the word do not fit in the line
                if (widthBuffer[1] + widthBuffer[0] > width && widthBuffer[1] > 0f) {
                    //empty the line buffer to the list
                    list.add(textBuffer[1]);
                    //reset the line buffer
                    textBuffer[1] = Text.empty();
                    widthBuffer[1] = 0f;
                }

                //skip if the word is empty in an empty line
                if (widthBuffer[0] <= 0f && widthBuffer[1] <= 0f)
                    continue;

                //word is too big!
                while (widthBuffer[0] > width) {
                    //if the word is longer than the width, we need to split it
                    Text subText = TextUtils.clampToWidth(textBuffer[0], width);

                    //if the subtext is empty, add only one char
                    if (subText.asString().isEmpty())
                        subText = substring(textBuffer[0], 0, 1);

                    //if the subtext (trimmed) is empty, skip this line
                    if (subText.asString().trim().isEmpty())
                        continue;

                    //add the subtext to the list as a line
                    list.add(subText);

                    //add the rest of the word to the current text
                    textBuffer[0] = substring(textBuffer[0], subText.asString().length(), textBuffer[0].asString().length());
                    widthBuffer[0] = f.width(textBuffer[0]);
                }

                //if we added all the word, skip
                if (widthBuffer[0] <= 0f)
                    continue;

                //add the word
                textBuffer[1].append(textBuffer[0]).append(t);
                widthBuffer[1] += widthBuffer[0] + w; //include the space

                //reset the word buffer
                textBuffer[0] = Text.empty();
                widthBuffer[0] = 0f;
            }
        }, Style.EMPTY);

        //append last line
        list.add(textBuffer[1]);
        return list;
    }

    public static Text join(List<Text> texts, Text separator) {
        if (texts.isEmpty())
            return Text.empty();

        Text result = texts.getFirst();
        for (int i = 1; i < texts.size(); i++)
            result.append(separator).append(texts.get(i));
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
        Font f = text.getStyle().getGuiStyle().getFont();
        for (Text t : split)
            width = Math.max(width, f.width(t));

        return (int) width;
    }

    public static int getHeight(Text text) {
        String[] split = text.asString().split("\n", -1);
        Font f = text.getStyle().getGuiStyle().getFont();
        return (int) (f.lineHeight * split.length + f.lineGap * (split.length - 1));
    }
}
