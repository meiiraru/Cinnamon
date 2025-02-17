package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CPFScreen extends ParentedScreen {

    private static final Pattern REGEX = Pattern.compile("(\\d)(\\d)(\\d)\\.(\\d)(\\d)(\\d)\\.(\\d)(\\d)(\\d)-(\\d)(\\d)");

    public CPFScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        WidgetList list = new WidgetList(width / 2, height / 2, width - 8, height - 8, 4);
        list.setAlignment(Alignment.CENTER);
        addWidget(list);

        //field
        TextField field = new TextField(0, 0, 180, 20);
        field.setHintText("CPF...");
        field.setCharLimit(11);
        field.setFilter(TextField.Filter.NUMBERS);
        field.setListener(s -> {
            String cpf = field.getFormattedText();
            if (cpf.length() != 14) {
                field.setBorderColor((Integer) null);
                field.setTooltip(null);
                return;
            }

            switch (checkCPF(cpf)) {
                case 1 -> {
                    field.setBorderColor(Colors.LIME);
                    field.setTooltip(Text.of("Valid CPF"));
                }
                case 2 -> {
                    field.setBorderColor(Colors.RED);
                    field.setTooltip(Text.of("Invalid CPF"));
                }
            }
        });
        field.setFormatting("***.***.***-**");
        list.addWidget(field);

        //validate button
        Button button = new Button(0, 0, 180, 20, Text.of("Validate"), b -> {
            String cpf = field.getFormattedText();
            Toast.addToast(Text.of("Checking...")).length(5);
            switch (checkCPF(cpf)) {
                case 1 -> field.setTooltip(Text.of("Valid CPF"));
                case 2 -> field.setTooltip(Text.of("Invalid CPF"));
                case -1 -> {
                    field.setBorderColor(Colors.YELLOW);
                    field.setTooltip(Text.of("Malformed CPF"));
                }
            }
        });
        list.addWidget(button);
        field.setEnterListener(tf -> button.onRun());

        super.init();
    }

    private static int checkCPF(String cpf) {
        try {
            if (isValid(cpf)) {
                return 1;
            } else {
                return 2;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean isValid(String cpf) {
        int[] digits = parseCPF(cpf);
        int a = calculateDigit(digits, 9, 10);
        int b = calculateDigit(digits, 10, 11);
        return a == digits[9] && b == digits[10];
    }

    private static int[] parseCPF(String cpf) {
        Matcher m = REGEX.matcher(cpf);
        if (cpf.length() != 14 || !m.matches())
            throw new IllegalArgumentException("Invalid CPF \"" + cpf + "\"");

        int[] digits = new int[11];
        for (int i = 0; i < digits.length; i++)
            digits[i] = Integer.parseInt(m.group(i + 1));

        return digits;
    }

    private static int calculateDigit(int[] digits, int count, int startingWeight) {
        int sum = 0;
        for (int i = 0; i < count; i++)
            sum += digits[i] * (startingWeight - i);
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
