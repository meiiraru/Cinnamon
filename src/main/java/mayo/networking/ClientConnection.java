package mayo.networking;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import mayo.networking.packet.Packet;

public class ClientConnection {

    public static Client connection;

    public static void connectToServer(String ip, int tcpPort, int udpPort, int timeout) {
        disconnect();

        try {
            Client client = new Client();
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
                    if (object instanceof Packet p)
                        p.clientReceived(client, connection);
                }
            });

            connection = client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void disconnect() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
