package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.gui.widgets.types.WidgetList;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.TextUtils;
import mayo.world.World;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        //may~o
        Text may = Text.of("May~o Renderer v0.1 \u25E0\u25DE\u25DF\u25E0").withStyle(Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK).shadow(true));
        this.addWidget(new Label(may, font, 4, height - TextUtils.getHeight(may, font) - 4));

        //buttons
        WidgetList list = new WidgetList(0, 0, 4);

        //open world
        Button worldButton = new Button(0, 0, 180, 20, Text.of("Open world"), () -> {
            World world = new World();
            world.init();
        });
        list.addWidget(worldButton);

        //dvd screen
        Button dvd = new Button(0, 0, 180, 20, Text.of("DVD screensaver"), () -> client.setScreen(new DVDScreen(this)));
        list.addWidget(dvd);

        //collision screen
        Button coll = new Button(0, 0, 180, 20, Text.of("Collision Test"), () -> client.setScreen(new CollisionScreen(this)));
        list.addWidget(coll);

        //curves screen
        Button curve = new Button(0, 0, 180, 20, Text.of("Curves!"), () -> client.setScreen(new Curves(this)));
        list.addWidget(curve);

        //close application
        Button exitButton = new Button(0, 0, 180, 20, Text.of("Exit"), () -> client.window.exit());
        exitButton.setTooltip(Text.of("bye~"));
        list.addWidget(exitButton);

        //add list to screen
        list.setPos((width - list.getWidth()) / 2, (height - list.getHeight()) / 2);
        this.addWidget(list);
    }
}
