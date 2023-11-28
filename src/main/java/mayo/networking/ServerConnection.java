package mayo.networking;

import mayo.world.WorldServer;

public class ServerConnection {

    public static WorldServer internalServer;

    public static void open() {
        close();

        //server world
        WorldServer server = new WorldServer();
        server.init();

        //open to lan
        server.openToLAN();

        //then connect to localhost
        ClientConnection.connectToServer(NetworkConstants.LOCAL_IP, NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT, 5000);

        //save server
        internalServer = server;
    }

    public static void close() {
        if (internalServer != null)
            internalServer.close();
    }
}
