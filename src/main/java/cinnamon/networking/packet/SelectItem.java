package cinnamon.networking.packet;

/*
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import cinnamon.networking.ServerConnection;
import cinnamon.world.World;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;

public class SelectItem extends PacketWithOwner {

    private int index;

    public SelectItem index(int index) {
        this.index = index;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        apply(cinnamon.Client.getInstance().world);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {
        apply(ServerConnection.world);
        server.sendToAllExceptUDP(connection.getID(), this);
    }

    private void apply(World world) {
        Entity e = world.getEntityByUUID(uuid);
        if (e instanceof LivingEntity le)
            le.setSelectedItem(index);
    }
}
 */