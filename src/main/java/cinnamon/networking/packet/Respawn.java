package cinnamon.networking.packet;

/*
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import cinnamon.networking.ServerConnection;
import cinnamon.world.entity.Entity;

public class Respawn extends PacketWithOwner {
    @Override
    public void clientReceived(Client client, Connection connection) {}

    @Override
    public void serverReceived(Server server, Connection connection) {
        int id = connection.getID();

        //add player
        Entity e = ServerConnection.world.addPlayer(id, name, uuid);
        server.sendToAllExceptTCP(id, new AddEntity().entity(e));
    }
}
 */