package cinnamon.utils;

import cinnamon.text.Style;
import cinnamon.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;

public class MarkdownParser {

    //types bitmask constants
    public static final byte BOLD       = 1;
    public static final byte ITALIC     = 1 << 1;
    public static final byte UNDERLINE  = 1 << 2;
    public static final byte STRIKE     = 1 << 3;
    public static final byte OBFUSCATED = 1 << 4;
    public static final byte DROP       = 1 << 5;

    public static Text parseMarkdown(Text text) {
        String raw = text.asString();
        int len = raw.length();

        //a single array tracking all styles and dropped characters
        byte[] styleMap = new byte[len];

        Deque<Integer> boldStack        = new ArrayDeque<>();
        Deque<Integer> italicStarStack  = new ArrayDeque<>();
        Deque<Integer> italicUnderStack = new ArrayDeque<>();
        Deque<Integer> underlineStack   = new ArrayDeque<>();
        Deque<Integer> strikeStack      = new ArrayDeque<>();
        Deque<Integer> obfStack         = new ArrayDeque<>();

        //pass 1 - pre scan the string to identify tokens and mark them in the styleMap
        for (int i = 0; i < len; i++) {
            char c = raw.charAt(i);
            char next = (i + 1 < len) ? raw.charAt(i + 1) : '\0';
            char prev = (i > 0) ? raw.charAt(i - 1) : ' ';

            if (c == '\\' && (next == '*' || next == '_' || next == '~' || next == '|' || next == '\\')) {
                styleMap[i] |= DROP;
                i++;
                continue;
            }

            if (c == '~' && next == '~') {
                handleToken(strikeStack, i, 2, STRIKE, styleMap);
                i++;
            } else if (c == '*' && next == '*') {
                handleToken(boldStack, i, 2, BOLD, styleMap);
                i++;
            } else if (c == '_' && next == '_') {
                handleToken(underlineStack, i, 2, UNDERLINE, styleMap);
                i++;
            } else if (c == '|' && next == '|') {
                handleToken(obfStack, i, 2, OBFUSCATED, styleMap);
                i++;
            } else if (c == '*') {
                handleToken(italicStarStack, i, 1, ITALIC, styleMap);
            } else if (c == '_') {
                if (!Character.isLetterOrDigit(prev) || !Character.isLetterOrDigit(next)) {
                    handleToken(italicUnderStack, i, 1, ITALIC, styleMap);
                }
            }
        }

        //pass 2 - build the final text with styles applied
        Text result = Text.empty();
        int[] globalIdx = {0};
        Style[] currentStyle = {null};
        StringBuilder currentText = new StringBuilder();

        Runnable flush = () -> {
            if (!currentText.isEmpty() && currentStyle[0] != null) {
                result.append(Text.of(currentText.toString()).withStyle(currentStyle[0]));
                currentText.setLength(0);
            }
        };

        text.visit((string, style) -> {
            for (int i = 0; i < string.length(); i++) {
                int g = globalIdx[0]++;
                if (g >= len)
                    break;

                byte mask = styleMap[g];

                //skip characters we flagged as delimiters or escapes
                if ((mask & DROP) != 0)
                    continue;

                Style charStyle = style;

                //apply formats
                if ((mask & BOLD) != 0)       charStyle = charStyle.bold(true);
                if ((mask & ITALIC) != 0)     charStyle = charStyle.italic(true);
                if ((mask & UNDERLINE) != 0)  charStyle = charStyle.underlined(true);
                if ((mask & STRIKE) != 0)     charStyle = charStyle.strikethrough(true);
                if ((mask & OBFUSCATED) != 0) charStyle = charStyle.obfuscated(true);

                if (currentStyle[0] != null && !currentStyle[0].equals(charStyle))
                    flush.run();

                currentStyle[0] = charStyle;
                currentText.append(string.charAt(i));
            }
        }, Style.EMPTY);

        flush.run();
        return result;
    }

    private static void handleToken(Deque<Integer> stack, int index, int len, byte styleBit, byte[] styleMap) {
        if (!stack.isEmpty()) {
            int start = stack.pop();
            //apply the style to the characters between the tokens
            for (int j = start; j < index + len; j++) {
                styleMap[j] |= styleBit;
            }
            //mark the tokens themselves to be hidden
            for (int j = 0; j < len; j++) {
                styleMap[start + j] |= DROP;
                styleMap[index + j] |= DROP;
            }
        } else {
            stack.push(index);
        }
    }

    public static Text escapeMarkdown(Text text) {
        return TextUtils.replaceAll(text, "([\\\\*_~|])", "\\\\$1");
    }
}