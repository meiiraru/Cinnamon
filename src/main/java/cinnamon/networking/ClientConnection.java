package cinnamon.networking;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import cinnamon.networking.packet.Packet;

import static cinnamon.Client.LOGGER;

public class ClientConnection {

    public static Client connection;

    public static boolean connectToServer(String ip, int tcpPort, int udpPort, int timeout) {
        disconnect();

        try {
            Client client = new Client(Client.DEFAULT_WRITE_BUFFER_SIZE, 8192); //TODO - Change to default value once chunks are implemented
            PacketRegistry.register(client.getKryo());
            client.start();
            client.connect(timeout, ip, tcpPort, udpPort);

            client.addListener(new Listener() {
                @Override
                public void disconnected(Connection connection) {
                    cinnamon.Client.getInstance().disconnect();
                }

                @Override
                public void received(Connection connection, Object object) {
                    try {
                        if (object instanceof Packet p)
                            p.clientReceived(client, connection);
                        else if (!(object instanceof FrameworkMessage))
                            LOGGER.warn("Unknown packet {}", object);
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse packet {}", object, e);
                    }
                }
            });

            connection = client;
        } catch (Exception e) {
            LOGGER.error("", e);
            return false;
        }

        return true;
    }

    public static void disconnect() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
