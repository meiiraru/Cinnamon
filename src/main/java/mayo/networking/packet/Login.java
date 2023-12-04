package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.networking.ServerConnection;
import mayo.world.entity.Entity;

public class Login extends PacketWithOwner {
    @Override
    public void clientReceived(Client client, Connection connection) {}

    @Override
    public void serverReceived(Server server, Connection connection) {
        int id = connection.getID();
        System.out.printf("[Server] %s (id %s) logged!\n", name, id);

        //join message
        server.sendToAllExceptTCP(id, new Message().msg(name + " joined the server"));

        //send terrain
        server.sendToTCP(id, new SendTerrain().terrain(ServerConnection.world.getTerrain()));

        //send entities
        server.sendToTCP(id, new SendEntities().entity(ServerConnection.world.getEntities().values()));

        //add player
        Entity e = ServerConnection.world.addPlayer(id, name, uuid);
        server.sendToAllExceptTCP(id, new AddEntity().entity(e));
    }
}
