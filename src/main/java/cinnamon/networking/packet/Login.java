package cinnamon.networking.packet;

/*
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import cinnamon.networking.ServerConnection;
import cinnamon.world.entity.Entity;

import static cinnamon.Client.LOGGER;

public class Login extends PacketWithOwner {
    @Override
    public void clientReceived(Client client, Connection connection) {}

    @Override
    public void serverReceived(Server server, Connection connection) {
        int id = connection.getID();
        LOGGER.info("[Server] %s (id %s) logged!", name, id);

        //join message
        server.sendToAllExceptTCP(id, new Message().msg(name + " joined the server"));

        //send terrain
        //server.sendToTCP(id, new SendTerrain().terrain(ServerConnection.world.getTerrain()));

        //send entities
        server.sendToTCP(id, new SendEntities().entity(ServerConnection.world.getEntities().values()));

        //add player
        Entity e = ServerConnection.world.addPlayer(id, name, uuid);
        server.sendToAllExceptTCP(id, new AddEntity().entity(e));
    }
}
 */