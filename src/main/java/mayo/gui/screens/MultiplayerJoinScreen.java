package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.widgets.ContainerList;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.TextField;
import mayo.networking.ClientConnection;
import mayo.networking.NetworkConstants;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.TextUtils;
import mayo.world.WorldClient;

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
        nameField = new TextField(x - 50, y - 15 - 4 - (int) font.lineHeight - 4 - 15, 100, 15, font);
        nameField.setListener(string -> name = string);
        nameField.setHintText("name...");
        nameField.setString(name);
        addWidget(nameField);

        //ip field
        ipField = new TextField(x - 50, y - 15, 100, 15, font);
        ipField.setListener(string -> ip = string);
        ipField.setHintText("ip...");
        ipField.setString(ip);
        addWidget(ipField);

        ContainerList list = new ContainerList(0, y + 4, 4, 2);

        //connect button
        list.addWidget(new Button(0, 0, 80, 20, Text.of("Connect"), button -> connect(name, ip)));

        //back button
        list.addWidget(new Button(0, 0, 80, 20, Text.of("Back"), button -> close()));

        list.setX(x - list.getWidth() / 2);
        addWidget(list);

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
        font.render(VertexConsumer.FONT, matrices, width / 2f,  nameField.getY() - 4 - font.lineHeight, Text.of("Enter your name:"), TextUtils.Alignment.CENTER);

        //ip text
        font.render(VertexConsumer.FONT, matrices, width / 2f, ipField.getY() - 4 - font.lineHeight, Text.of("Enter server IP:"), TextUtils.Alignment.CENTER);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void connect(String name, String ip) {
        if (name.isBlank()) {
            Toast.addToast(Text.of("Invalid name"), client.font);
            return;
        }

        client.setName(name);

        if (ClientConnection.connectToServer(ip, NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT, 10_000)) {
            WorldClient world = new WorldClient();
            world.init();
        } else {
            Toast.addToast(Text.of("Unable to connect to server"), client.font);
        }
    }
}
