package mayo.networking;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import mayo.networking.packet.Packet;
import mayo.world.WorldServer;

public class ServerConnection {

    public static WorldServer world;
    private static Server connection;

    public static boolean open() {
        close();

        //open server
        try {
            Server server = new Server(Server.DEFAULT_WRITE_BUFFER_SIZE, 8192); //TODO - Change to default value once chunks are implemented
            PacketRegistry.register(server.getKryo());
            server.start();
            server.bind(NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT);

            server.addListener(new Listener() {
                public void received (Connection connection, Object object) {
                    try {
                        if (object instanceof Packet p)
                            p.serverReceived(server, connection);
                    } catch (Exception e) {
                        System.out.println("Failed to parse packet " + object);
                        e.printStackTrace();
                    }
                }
            });

            connection = server;
        } catch (Exception e) {
            System.out.println("Unable to create local server");
            e.printStackTrace();
            close();
            return false;
        }

        //server world
        WorldServer worldServer = new WorldServer();
        worldServer.init();

        //save server
        world = worldServer;

        //then connect to localhost
        if (!ClientConnection.connectToServer(NetworkConstants.LOCAL_IP, NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT, 30_000)) {
            System.out.println("Failed to connect to local server");
            close();
            return false;
        }

        return true;
    }

    public static void close() {
        if (world != null) {
            world.close();
            world = null;
        }

        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
