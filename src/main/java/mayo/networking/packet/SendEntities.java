package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.world.WorldClient;
import mayo.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;

public class SendEntities implements Packet {

    private Collection<Entity> entities;

    public SendEntities entity(Collection<Entity> entities) {
        this.entities = new ArrayList<>(entities);
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        WorldClient world = mayo.Client.getInstance().world;
        for (Entity e : entities)
            world.addEntity(e);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {}
}
