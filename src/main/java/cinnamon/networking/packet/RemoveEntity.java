package cinnamon.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

import java.util.UUID;

public class RemoveEntity implements Packet {
    private UUID uuid;

    public RemoveEntity uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        cinnamon.Client.getInstance().world.getEntityByUUID(uuid).remove();
    }

    @Override
    public void serverReceived(Server server, Connection connection) {}
}
