package cinnamon.networking;

/*
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import cinnamon.networking.packet.Packet;
import cinnamon.world.WorldServer;

import static cinnamon.Client.LOGGER;

public class ServerConnection {

    public static WorldServer world;
    public static Server connection;

    public static boolean open() {
        close();

        //open server
        try {
            Server server = new Server(Server.DEFAULT_WRITE_BUFFER_SIZE, 8192); //TODO - Change to default value once chunks are implemented
            PacketRegistry.register(server.getKryo());
            server.start();
            server.bind(NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT);

            server.addListener(new Listener() {
                @Override
                public void disconnected(Connection connection) {
                    if (world != null)
                        world.removePlayer(connection.getID());
                }

                @Override
                public void received (Connection connection, Object object) {
                    try {
                        if (object instanceof Packet p)
                            p.serverReceived(server, connection);
                        else if (!(object instanceof FrameworkMessage))
                            LOGGER.warn("Unknown packet {}", object);
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse packet {}", object, e);
                    }
                }
            });

            connection = server;
        } catch (Exception e) {
            LOGGER.error("Unable to create local server", e);
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
            LOGGER.error("Failed to connect to local server");
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

    public static void tick() {
        if (world != null)
            world.tick();
    }
}
 */