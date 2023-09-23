package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.TextUtils;
import mayo.world.World;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        //open world
        Button worldButton = new Button((width - 180) / 2, (height - 20) / 2 - 20 - 16, 180, 20, Text.of("Open world"), () -> {
            client.world = new World();
            client.world.init();
            client.setScreen(null);
        });
        this.addWidget(worldButton);

        //dvd screen
        Button dvd = new Button(worldButton.getX(), worldButton.getY() + worldButton.getHeight() + 16, 180, 20, Text.of("DVD screensaver"), () -> client.setScreen(new DVDScreen(this)));
        this.addWidget(dvd);

        //close application
        Button exitButton = new Button(dvd.getX(), dvd.getY() + dvd.getHeight() + 16, 180, 20, Text.of("Exit"), () -> client.window.exit());
        exitButton.setTooltip(Text.of("bye~"));
        this.addWidget(exitButton);

        //may~o
        Text may = Text.of("May~o Renderer v0.1").withStyle(Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK));
        this.addWidget(new Label(may, font, 4, height - TextUtils.getHeight(may, font) - 4));
    }
}
