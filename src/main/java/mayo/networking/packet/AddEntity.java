package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.world.entity.Entity;

public class AddEntity implements Packet {

    private Entity e;

    public AddEntity entity(Entity e) {
        this.e = e;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        mayo.Client.getInstance().world.addEntity(e);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {}
}