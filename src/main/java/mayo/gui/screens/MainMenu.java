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
            World world = new World();
            world.init();
        });
        this.addWidget(worldButton);

        //dvd screen
        Button dvd = new Button(worldButton.getX(), worldButton.getY() + worldButton.getHeight() + 16, 180, 20, Text.of("DVD screensaver"), () -> client.setScreen(new DVDScreen(this)));
        this.addWidget(dvd);

        //collision screen
        Button coll = new Button(dvd.getX(), dvd.getY() + dvd.getHeight() + 16, 180, 20, Text.of("Collision Test"), () -> client.setScreen(new CollisionScreen(this)));
        this.addWidget(coll);

        //close application
        Button exitButton = new Button(coll.getX(), coll.getY() + coll.getHeight() + 16, 180, 20, Text.of("Exit"), () -> client.window.exit());
        exitButton.setTooltip(Text.of("bye~"));
        this.addWidget(exitButton);

        //may~o
        Text may = Text.of("May~o Renderer v0.1 \u25E0\u25DE\u25DF\u25E0").withStyle(Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK));
        this.addWidget(new Label(may, font, 4, height - TextUtils.getHeight(may, font) - 4));
    }
}
