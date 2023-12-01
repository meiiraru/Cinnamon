package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.networking.ServerConnection;
import mayo.world.entity.Entity;

public class Respawn extends PacketWithOwner {
    @Override
    public void clientReceived(Client client, Connection connection) {}

    @Override
    public void serverReceived(Server server, Connection connection) {
        int id = connection.getID();

        //add player
        Entity e = ServerConnection.world.addPlayer(id, uuid);
        server.sendToAllExceptTCP(id, new AddEntity().entity(e));
    }
}
