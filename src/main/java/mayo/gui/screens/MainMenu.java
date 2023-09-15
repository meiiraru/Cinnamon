package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Label;
import mayo.text.Text;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        Label l = new Label(Text.of("TEST"), client.font, 20, 20);
        this.addWidget(l);
        l.setX(width - l.getWidth());
    }
}
