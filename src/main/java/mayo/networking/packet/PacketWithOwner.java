package mayo.networking.packet;

import mayo.Client;

import java.util.UUID;

public abstract class PacketWithOwner implements Packet {

    public final String name;
    public final UUID uuid;

    public PacketWithOwner() {
        this.name = Client.getInstance().name;
        this.uuid = Client.getInstance().playerUUID;
    }
}
