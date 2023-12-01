package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class Handshake extends PacketWithOwner {

    private final String version;

    public Handshake() {
        super();
        this.version = mayo.Client.VERSION;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        System.out.println("Handshake received!");
    }

    @Override
    public void serverReceived(Server server, Connection connection) {
        System.out.printf("[Server] %s (id %s) handshaked with version: v%s\n", name, connection.getID(), version);
        connection.sendTCP(new Handshake());
    }
}
