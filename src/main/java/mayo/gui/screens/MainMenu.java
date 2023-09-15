package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.text.Text;
import mayo.world.World;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        //open world
        Button worldButton = new Button((width - 160) / 2, (height - 20) / 2 - 20 - 16, 160, 20, Text.of("Open world"), () -> {
            client.world = new World();
            client.world.init();
            client.setScreen(null);
        });
        this.addWidget(worldButton);

        //dvd screen
        Button dvd = new Button(worldButton.getX(), worldButton.getY() + worldButton.getHeight() + 16, 160, 20, Text.of("DVD"), () -> client.setScreen(new DVDScreen(this)));
        this.addWidget(dvd);

        //close application
        Button exitButton = new Button(dvd.getX(), dvd.getY() + dvd.getHeight() + 16, 160, 20, Text.of("Exit"), () -> client.window.exit());
        this.addWidget(exitButton);
    }
}
