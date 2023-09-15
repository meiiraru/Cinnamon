package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.text.Text;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        Label l = new Label(Text.of("TEST"), client.font, 20, 20);
        l.setX(width - l.getWidth());
        this.addWidget(l);

        Button butt = new Button(0, 0, 60, 20, Text.of("BUTTON"), () -> {
            l.setText(Text.of("THE BUTTON HAS BEEN PRESSED!!!"));
            l.setX(width - l.getWidth());
        });
        butt.setPos((width - butt.getWidth()) / 2, (height - butt.getHeight()) / 2);
        this.addWidget(butt);
    }
}
