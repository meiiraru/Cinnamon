package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.networking.ServerConnection;

public class Login implements Packet {

    private final String name;

    public Login() {
        this.name = mayo.Client.PLAYERNAME;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {}

    @Override
    public void serverReceived(Server server, Connection connection) {
        int id = connection.getID();
        System.out.printf("[Server] %s (id %s) logged!\n", name, id);

        //join message
        server.sendToAllExceptTCP(id, new Message().msg(name + " joined the server"));

        //send world
        server.sendToUDP(id, new SendTerrain().terrain(ServerConnection.world.getTerrain()));
    }
}
