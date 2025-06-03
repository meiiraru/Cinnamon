package cinnamon.gui.screens;

import cinnamon.gui.GUIStyle;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.TextField;
//import cinnamon.networking.ClientConnection;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.world.world.WorldClient;

public class MultiplayerJoinScreen extends ParentedScreen {

    private static String name = "", ip = "";

    private TextField nameField, ipField;

    public MultiplayerJoinScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        int x = width / 2;
        int y = height / 2;

        //name field
        nameField = new TextField(x - 50, y - 15 - 4 - (int) GUIStyle.getDefault().getFont().lineHeight - 4 - 15, 100, 15);
        nameField.setListener(string -> name = string);
        nameField.setHintText(Text.translated("gui.multiplayer.name_hint"));
        nameField.setText(name);
        addWidget(nameField);

        //ip field
        ipField = new TextField(x - 50, y - 15, 100, 15);
        ipField.setListener(string -> ip = string);
        ipField.setHintText(Text.translated("gui.multiplayer.ip_hint"));
        ipField.setText(ip);
        addWidget(ipField);

        ContainerGrid grid = new ContainerGrid(0, y + 4, 4, 2);

        //connect button
        grid.addWidget(new Button(0, 0, 80, 20, Text.translated("gui.multiplayer.connect"), button -> connect(name, ip)));

        //back button
        grid.addWidget(new Button(0, 0, 80, 20, Text.translated("gui.back"), button -> close()));

        grid.setX(x - grid.getWidth() / 2);
        addWidget(grid);

        super.init();
    }

    @Override
    protected void addBackButton() {
        //added in the init
        //super.addBackButton();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //name text
        Text.translated("gui.multiplayer.name").render(VertexConsumer.FONT, matrices, width / 2f,  nameField.getY() - 4 - GUIStyle.getDefault().getFont().lineHeight, Alignment.TOP_CENTER);

        //ip text
        Text.translated("gui.multiplayer.ip").render(VertexConsumer.FONT, matrices, width / 2f, ipField.getY() - 4 - GUIStyle.getDefault().getFont().lineHeight, Alignment.TOP_CENTER);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void connect(String name, String ip) {
        if (name.isBlank()) {
            Toast.addToast(Text.translated("gui.multiplayer.name_invalid"));
            return;
        }

        client.setName(name);

        if (false) { //ClientConnection.connectToServer(ip, NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT, 10_000)) {
            WorldClient world = new WorldClient();
            world.init();
        } else {
            Toast.addToast(Text.translated("gui.multiplayer.connect_error"));
        }
    }
}
