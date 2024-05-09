package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

import static mayo.Client.LOGGER;

public class Handshake extends PacketWithOwner {

    private final String version;

    public Handshake() {
        super();
        this.version = mayo.Client.VERSION;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        LOGGER.info("Handshake received!");
    }

    @Override
    public void serverReceived(Server server, Connection connection) {
        LOGGER.info("[Server] {} (id {}) handshaked with version: v{}", name, connection.getID(), version);
        connection.sendTCP(new Handshake());
    }
}
