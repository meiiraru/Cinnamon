package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.text.Text;
import mayo.world.World;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        Button worldButton = new Button(0, 0, 160, 20, Text.of("Open world"), () -> {
            client.world = new World();
            client.world.init();
            client.setScreen(null);
        });
        worldButton.setPos((width - worldButton.getWidth()) / 2, (height - worldButton.getHeight()) / 2);
        this.addWidget(worldButton);

        Button exitButton = new Button(0, 0, 160, 20, Text.of("Exit"), () -> client.exit());
        exitButton.setPos(worldButton.getX(), worldButton.getY() + worldButton.getHeight() + 16);
        this.addWidget(exitButton);
    }
}
