package mayo.gui.screens.extras;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.widgets.WidgetList;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.TextField;
import mayo.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CPFScreen extends ParentedScreen {

    private static final Pattern REGEX = Pattern.compile("(\\d)(\\d)(\\d)\\.(\\d)(\\d)(\\d)\\.(\\d)(\\d)(\\d)-(\\d)(\\d)");

    public CPFScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        WidgetList list = new WidgetList(0, 0, 0, 0, 4);
        list.setShouldRenderBackground(false);

        //field
        TextField field = new TextField(0, 0, 180, 20, font);
        field.setHintText("CPF...");
        field.setFilter(s -> s.matches("^\\d{0,3}(\\.\\d{0,3})?(\\.\\d{0,3})?(-\\d{0,2})?$"));
        list.addWidget(field);

        //validate button
        list.addWidget(new Button(0, 0, 180, 20, Text.of("Validate"), b -> {
            String cpf = field.getText();
            try {
                if (isValid(cpf))
                    Toast.addToast(Text.of("Valid CPF"), font);
                else
                    Toast.addToast(Text.of("Invalid CPF"), font);
            } catch (Exception e) {
                Toast.addToast(Text.of("Malformed CPF"), font);
            }
        }));

        //add list to screen
        list.setDimensions(width - 8, Math.min(list.getWidgetsHeight(), height - 8));
        list.setPos(width / 2, (height - list.getHeight()) / 2);
        this.addWidget(list);

        super.init();
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
