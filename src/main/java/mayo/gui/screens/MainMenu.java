package mayo.gui.screens;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.gui.widgets.types.WidgetList;
import mayo.networking.NetworkConstants;
import mayo.networking.ClientConnection;
import mayo.networking.ServerConnection;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.TextUtils;
import mayo.world.WorldClient;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        //may~o
        Text may = Text.of("May~o Renderer v%s \u25E0\u25DE\u25DF\u25E0".formatted(Client.VERSION)).withStyle(Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK).shadow(true));
        this.addWidget(new Label(may, font, 4, height - TextUtils.getHeight(may, font) - 4));

        //buttons
        WidgetList list = new WidgetList(0, 0, 4);

        //open world
        Button worldButton = new Button(0, 0, 180, 20, Text.of("Open world").withStyle(Style.EMPTY.color(Colors.YELLOW)), button -> {
            //init client
            WorldClient world = new WorldClient();
            ServerConnection.open();
            world.init();
        });
        list.addWidget(worldButton);

        //join world
        Button joinWorld = new Button(0, 0, 180, 20, Text.of("Join world (mp)").withStyle(Style.EMPTY.color(Colors.BLUE)), button -> {
            WorldClient world = new WorldClient();
            world.init();
            ClientConnection.connectToServer(NetworkConstants.LOCAL_IP, NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT, 5000);
        });
        list.addWidget(joinWorld);

        //dvd screen
        Button dvd = new Button(0, 0, 180, 20, Text.of("DVD screensaver"), button -> client.setScreen(new DVDScreen(this)));
        list.addWidget(dvd);

        //collision screen
        Button coll = new Button(0, 0, 180, 20, Text.of("Collision Test"), button -> client.setScreen(new CollisionScreen(this)));
        list.addWidget(coll);

        //curves screen
        Button curve = new Button(0, 0, 180, 20, Text.of("Curves").withStyle(Style.EMPTY.color(Colors.YELLOW)), button -> client.setScreen(new CurvesScreen(this)));
        list.addWidget(curve);

        //close application
        Button exitButton = new Button(0, 0, 180, 20, Text.of("Exit"), button -> client.window.exit());
        exitButton.setTooltip(Text.of("bye~"));
        list.addWidget(exitButton);

        //add list to screen
        list.setPos((width - list.getWidth()) / 2, (height - list.getHeight()) / 2);
        this.addWidget(list);
    }
}
