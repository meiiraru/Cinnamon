package cinnamon.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import cinnamon.networking.ServerConnection;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.UUID;

public class ClientMovement implements Packet {

    private UUID uuid;
    private Vector3f movement;
    private Vector2f rotation;
    private boolean sneak, sprint, flying;

    public ClientMovement uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public ClientMovement move(float left, float up, float forwards) {
        this.movement = new Vector3f(left, up, forwards);
        return this;
    }

    public ClientMovement rotate(float pitch, float yaw) {
        this.rotation = new Vector2f(pitch, yaw);
        return this;
    }

    public ClientMovement flags(boolean sneak, boolean sprint, boolean flying) {
        this.sneak = sneak;
        this.sprint = sprint;
        this.flying = flying;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {}

    @Override
    public void serverReceived(Server server, Connection connection) {
        Entity e = ServerConnection.world.getEntityByUUID(uuid);
        if (e == null)
            return;

        if (movement != null)
            e.move(movement.x, movement.y, movement.z);

        if (rotation != null)
            e.rotate(rotation.x, rotation.y);

        if (e instanceof Player p)
            p.updateMovementFlags(sneak, sprint, flying);
    }
}
