package mayo.gui.screens;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.widgets.TextField;

public class MainMenu extends Screen {

    public MainMenu() {
        addWidget(new TextField(Client.getInstance().font, "May-o renderer \u25E0\u25DE\u25DF\u25E0"));
    }
}
