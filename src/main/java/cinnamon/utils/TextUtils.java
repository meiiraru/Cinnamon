package cinnamon.utils;

import cinnamon.render.Font;
import cinnamon.text.Formatting;
import cinnamon.text.Style;
import cinnamon.text.Text;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for manipulation and formatting of {@link Text} objects
 */
public final class TextUtils {

    private static final Text
            ELLIPSIS = Text.of("..."),
            NEW_LINE = Text.of("\n");

    private TextUtils() {}

    /**
     * Splits a {@link Text} object into a list of {@link Text} objects based on the provided {@code regex}
     * @param text The {@link Text} object to split
     * @param regex The {@code regex} to split the text by
     * @return A list of {@link Text} objects resulting from the split operation
     */
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

    /**
     * Reverses the order of characters in a {@link Text} object
     * @param text The {@link Text} object to reverse
     * @return A new {@link Text} object with the characters in reverse order
     */
    public static Text reverse(Text text) {
        Text[] builder = {Text.empty()};
        text.visit((string, style) -> {
            StringBuilder str = new StringBuilder(string).reverse();
            builder[0] = Text.of(str).withStyle(style).append(builder[0]);
        }, Style.EMPTY);
        return builder[0];
    }

    /**
     * Returns a substring of the given {@link Text} object, starting from {@code beginIndex} (inclusive) and ending at {@code endIndex} (exclusive)
     * @param text The {@link Text} object to extract the substring from
     * @param beginIndex The starting index (inclusive) of the substring
     * @param endIndex The ending index (exclusive) of the substring
     * @return A new {@link Text} object representing the substring of the original text
     */
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

    /**
     * Trims leading and trailing whitespace from the given {@link Text} object
     * @param text The {@link Text} object to trim
     * @return A new {@link Text} object with leading and trailing whitespace removed
     */
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

    /**
     * Adds an ellipsis {@code "..."} to the end of the given {@link Text} object if its width exceeds the specified {@code width}
     * @param text The {@link Text} object to potentially add an ellipsis to
     * @param width The maximum width allowed for the text before adding an ellipsis
     * @return A new {@link Text} object with an ellipsis added if the original text exceeds the specified width, otherwise returns the original text
     */
    public static Text addEllipsis(Text text, float width) {
        if (getWidth(text) <= width)
            return text;

        Text ellipsis = Text.empty().withStyle(text.getStyle()).append(ELLIPSIS);
        int ellipsisWidth = getWidth(ellipsis);

        Text clamped = clampToWidth(text, width - ellipsisWidth);
        clamped.append(ellipsis);

        return clamped;
    }

    /**
     * Parses {@link Formatting} codes in the given {@link Text} object and applies the corresponding styles to the text segments
     * @param text The {@link Text} object containing formatting codes to parse
     * @return A new {@link Text} object with the formatting codes applied as styles to the text segments
     */
    public static Text parseColorFormatting(Text text) {
        Text result = Text.empty();

        text.visit((string, style) -> {
            StringBuilder currentText = new StringBuilder();
            Style currentStyle = style;

            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);

                //check for a formatting character
                if (c == Formatting.FORMATTING_CHAR && i + 1 < string.length()) {
                    char next = string.charAt(i + 1);

                    //escape double formatting
                    if (next == Formatting.FORMATTING_CHAR) {
                        currentText.append(next);
                        i++; //skip the escaped formatting char
                        continue;
                    }

                    //try to parse it as a valid formatting
                    Formatting formatting = Formatting.byCode(next);
                    if (formatting != null) {
                        //formatting changing - flush the current text
                        if (!currentText.isEmpty()) {
                            result.append(Text.empty().withStyle(style).append(Text.of(currentText.toString()).withStyle(currentStyle)));
                            currentText.setLength(0);
                        }

                        if (formatting == Formatting.HEX) {
                            //ensure there are 6 characters left for the hex code
                            if (i + 7 < string.length()) {
                                String hex = string.substring(i + 2, i + 8);
                                int color = ColorUtils.rgbToInt(ColorUtils.hexStringToRGB(hex));
                                currentStyle = currentStyle.color(color + 0xFF000000);
                                i += 7; //skip the formatting char and the 6 hex digits
                            } else {
                                currentText.append(c); //not enough characters, treat as literal text
                            }
                        } else {
                            //standard formatting
                            currentStyle = currentStyle.formatted(formatting);
                            i++; //skip the formatting code itself
                        }

                        continue;
                    }
                }

                //appen on none or invalid formatting code
                currentText.append(c);
            }

            //flush leftovers
            if (!currentText.isEmpty()) {
                result.append(Text.empty().withStyle(style).append(Text.of(currentText.toString()).withStyle(currentStyle)));
                currentText.setLength(0);
            }
        }, Style.EMPTY);

        return result;
    }

    /**
     * Replaces all occurrences of the specified {@code regex} in the given {@link Text} object with the provided {@code replacement} string
     * @param text The {@link Text} object in which to perform the replacement
     * @param regex The regular expression to match occurrences for replacement
     * @param replacement The string to replace each matched occurrence with
     * @return A new {@link Text} object with all occurrences of the specified regex replaced by the replacement string
     */
    public static Text replaceAll(Text text, String regex, String replacement) {
        Text result = Text.empty();
        Pattern pattern = Pattern.compile(regex);

        text.visit((string, style) -> {
            Matcher matcher = pattern.matcher(string);
            StringBuilder sb = new StringBuilder();

            while (matcher.find())
                matcher.appendReplacement(sb, replacement);
            matcher.appendTail(sb);

            result.append(Text.of(sb).withStyle(style));
        }, Style.EMPTY);

        return result;
    }

    /**
     * Clamps the given {@link Text} object to fit within the specified {@code width}, truncating any text that exceeds the width
     * @param text The {@link Text} object to clamp
     * @param width The maximum width allowed for the text
     * @return A new {@link Text} object that fits within the specified width, with any excess text truncated
     * @see #clampToWidth(Text, float, boolean)
     */
    public static Text clampToWidth(Text text, float width) {
        return clampToWidth(text, width, false);
    }

    /**
     * Clamps the given {@link Text} object to fit within the specified {@code width}, truncating any text that exceeds the width<br>
     * If {@code roundToClosest} is true, the method will include the last character that exceeds the width if it is closer to the width than the previous character
     * @param text The {@link Text} object to clamp
     * @param width The maximum width allowed for the text
     * @param roundToClosest Whether to include the last character that exceeds the width if it is closer to the width than the previous character
     * @return A new {@link Text} object that fits within the specified width, with any excess text truncated
     * @see #clampToWidth(Text, float)
     */
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
            Font f = style.getGuiSkin().getFont();

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
            builder.append(Text.of(current).withStyle(style));
            return stop;
        }, Style.EMPTY);

        //return
        return builder;
    }

    /**
     * Wraps the given {@link Text} object into multiple lines based on the specified {@code width}, ensuring that each line does not exceed the width<br>
     * The method will split the text at spaces and, if necessary, break words that exceed the width into multiple lines
     * @param text The {@link Text} object to wrap into multiple lines
     * @param width The maximum width allowed for each line of text
     * @return A list of {@link Text} objects, each representing a line of text that fits within the specified width
     */
    public static List<Text> warpToWidth(Text text, float width) {
        List<Text> list = new ArrayList<>();
        Text toVisit = Text.empty().append(text).append(" ");

        //[0] word buffer, [1] line buffer
        Text[] textBuffer = {Text.empty(), Text.empty()};
        float[] widthBuffer = {0f, 0f};

        //iterate text
        toVisit.visit((s, style) -> {
            Font f = style.getGuiSkin().getFont();

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

    /**
     * Joins a list of {@link Text} objects into a single {@link Text} object, separating each element with a newline character
     * @param texts The list of {@link Text} objects to join
     * @return A new {@link Text} object containing the joined text elements, separated by newline characters
     * @see #join(List, Text)
     */
    public static Text join(List<Text> texts) {
        return join(texts, NEW_LINE);
    }

    /**
     * Joins a list of {@link Text} objects into a single {@link Text} object, separating each element with the specified {@code separator}
     * @param texts The list of {@link Text} objects to join
     * @param separator The {@link Text} object to use as a separator between each element in the joined text
     * @return A new {@link Text} object containing the joined text elements, separated by the specified separator
     * @see #join(List)
     */
    public static Text join(List<Text> texts, Text separator) {
        if (texts.isEmpty())
            return Text.empty();

        Text result = texts.getFirst();
        for (int i = 1; i < texts.size(); i++) {
            if (separator != null)
                result.append(separator).append(texts.get(i));
            else
                result.append(texts.get(i));
        }
        return result;
    }

    /**
     * Splits a {@link Text} object into a list of individual characters, preserving the styles of each character
     * @param text The {@link Text} object to split into characters
     * @return A list of {@link Text} objects, each representing a single character from the original text
     */
    public static List<Text> splitToChars(Text text) {
        List<Text> list = new ArrayList<>();
        text.visit((string, style) -> {
            for (int i = 0; i < string.length(); ) {
                int c = string.codePointAt(i);
                int count = Character.charCount(c);
                list.add(Text.of(string.substring(i, i + count)).withStyle(style));
                i += count;
            }
        }, Style.EMPTY);
        return list;
    }

    /**
     * Returns the {@link Style} of the character at the specified local coordinates ({@code localX}, {@code localY}) within the given {@link Text} object,
     * taking into account the specified {@link Alignment}
     * @param text The {@link Text} object to check for the character style
     * @param localX The local X coordinate within the text area
     * @param localY The local Y coordinate within the text area
     * @param alignment The {@link Alignment} to consider when determining the character style
     * @return The {@link Style} of the character at the specified coordinates, or {@code null} if the coordinates are outside the bounds of the text
     */
    public static Style getStyleAt(Text text, int localX, int localY, Alignment alignment) {
        Font f = text.getStyle().getGuiSkin().getFont();
        int lineHeight = (int) (f.lineHeight + f.lineGap);
        int lineIndex = (int) Math.floor(localY / (float) lineHeight);

        List<Text> lines = split(text, "\n");
        if (lineIndex < 0 || lineIndex >= lines.size())
            return null;

        Text line = lines.get(lineIndex);
        float currentX = -alignment.getWidthOffset(getWidth(text)) + alignment.getWidthOffset(getWidth(line));

        for (Text t : splitToChars(line)) {
            float charWidth = f.width(t);
            if (currentX <= localX && localX < currentX + charWidth)
                return t.getStyle();

            currentX += charWidth;
        }

        return null;
    }

    /**
     * Calculates the width of the given {@link Text} object, taking into account line breaks and the maximum width of each line
     * @param text The {@link Text} object for which to calculate the width
     * @return The width of the text in pixels, based on the maximum width of its lines
     * @see #getWidth(List)
     */
    public static int getWidth(Text text) {
        List<Text> split = split(text, "\n");
        return getWidth(split);
    }

    /**
     * Calculates the maximum width of a list of {@link Text} objects, taking into account the width of each individual text element
     * @param texts The list of {@link Text} objects for which to calculate the maximum width
     * @return The maximum width of the text elements in pixels
     * @see #getWidth(Text)
     */
    public static int getWidth(List<Text> texts) {
        float width = 0f;
        for (Text t : texts)
            width = Math.max(width, t.getStyle().getGuiSkin().getFont().width(t));

        return (int) width;
    }

    /**
     * Calculates the total height of the given {@link Text} object, taking into account the line height and line gap of the text font
     * @param text The {@link Text} object for which to calculate the height
     * @return The total height of the text in pixels, based on the number of lines and the line height and line gap of the font
     * @see #getHeight(List)
     */
    public static int getHeight(Text text) {
        String[] split = text.asString().split("\n", -1);
        Font f = text.getStyle().getGuiSkin().getFont();
        int lines = split.length;
        return (int) (f.lineHeight * lines + f.lineGap * (lines - 1));
    }

    /**
     * Calculates the total height of a list of {@link Text} objects, taking into account the line height and line gap of the text font
     * @param texts The list of {@link Text} objects for which to calculate the total height
     * @return The total height of the text elements in pixels, based on the sum of their line heights and line gaps
     * @see #getHeight(Text)
     */
    public static int getHeight(List<Text> texts) {
        float height = 0f;
        Font f = null;

        for (Text text : texts) {
            f = text.getStyle().getGuiSkin().getFont();
            height += f.lineHeight + f.lineGap;
        }

        //remove the last line gap
        if (f != null)
            height -= f.lineGap;

        return (int) height;
    }
}
