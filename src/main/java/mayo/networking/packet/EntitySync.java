package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.world.World;
import mayo.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.UUID;

public class EntitySync implements Packet {

    private UUID uuid;
    private Vector3f pos;
    private Vector2f rot;

    public EntitySync entity(Entity entity) {
        this.uuid = entity.getUUID();
        this.pos = new Vector3f(entity.getPos());
        this.rot = new Vector2f(entity.getRot());
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        World world = mayo.Client.getInstance().world;
        if (world == null)
            return;

        Entity e = world.getEntityByUUID(uuid);
        if (e == null)
            return;

        e.moveTo(pos);
        e.rotateTo(rot);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {}
}
