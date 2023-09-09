package mayo.utils;

import mayo.render.Font;
import mayo.text.Style;
import mayo.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class TextUtils {

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

    public enum Alignment {
        LEFT((font, text) -> 0),
        RIGHT((font, text) -> font.width(text)),
        CENTER((font, text) -> font.width(text) / 2);

        private final BiFunction<Font, Text, Integer> textFunction;

        Alignment(BiFunction<Font, Text, Integer> textFunction) {
            this.textFunction = textFunction;
        }

        public int apply(Font font, Text text) {
            return textFunction.apply(font, text);
        }
    }
}
