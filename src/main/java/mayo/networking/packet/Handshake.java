package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class Handshake implements Packet {

    private final String version, name;

    public Handshake() {
        this.version = mayo.Client.VERSION;
        this.name = mayo.Client.PLAYERNAME;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {}

    @Override
    public void serverReceived(Server server, Connection connection) {
        System.out.printf("[Server] %s (Client %s) handshaked with version: v%s\n", name, connection.getID(), version);
    }
}
