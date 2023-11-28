package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public interface Packet {
    void clientReceived(Client client, Connection connection);
    void serverReceived(Server server, Connection connection);
}
