package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.world.entity.Entity;
import mayo.world.entity.living.LivingEntity;

import java.util.UUID;

public class SyncHealth implements Packet {

    private UUID entity;
    private int health;

    public SyncHealth entity(UUID entity) {
        this.entity = entity;
        return this;
    }

    public SyncHealth health(int health) {
        this.health = health;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        Entity e = mayo.Client.getInstance().world.getEntityByUUID(entity);
        if (e instanceof LivingEntity le)
            le.setHealth(health);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {}
}
