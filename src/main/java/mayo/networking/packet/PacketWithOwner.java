package mayo.networking.packet;

import java.util.UUID;

public abstract class PacketWithOwner implements Packet {

    public final String name;
    public final UUID uuid;

    public PacketWithOwner() {
        this.name = mayo.Client.PLAYERNAME;
        this.uuid = mayo.Client.PLAYER_UUID;
    }
}
