package mayo.networking;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import mayo.networking.packet.Packet;
import mayo.networking.registry.PacketRegistry;

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
                    mayo.Client.getInstance().disconnect();
                }

                @Override
                public void received(Connection connection, Object object) {
                    try {
                        if (object instanceof Packet p)
                            p.clientReceived(client, connection);
                    } catch (Exception e) {
                        System.out.println("Failed to parse packet " + object);
                        e.printStackTrace();
                    }
                }
            });

            connection = client;
        } catch (Exception e) {
            e.printStackTrace();
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
