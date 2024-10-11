package cinnamon.networking.packet;

/*
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import cinnamon.networking.ServerConnection;
import cinnamon.world.World;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;

public class ClientEntityAction extends PacketWithOwner {

    private Boolean use, attack;

    public ClientEntityAction use(boolean use) {
        this.use = use;
        return this;
    }

    public ClientEntityAction attack(boolean attack) {
        this.attack = attack;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        apply(cinnamon.Client.getInstance().world);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {
        apply(ServerConnection.world);
        server.sendToAllUDP(this);
    }

    private void apply(World world) {
        Entity e = world.getEntityByUUID(uuid);
        if (e instanceof LivingEntity le) {
            if (use != null) {
                if (use) le.useAction();
                else le.stopUsing();
            }
            if (attack != null) {
                if (attack) le.attackAction();
                else le.stopAttacking();
            }
        }
    }
}
 */