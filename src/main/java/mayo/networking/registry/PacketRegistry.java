package mayo.networking.registry;

import com.esotericsoftware.kryo.Kryo;
import mayo.networking.packet.Handshake;
import mayo.networking.packet.Login;
import mayo.networking.packet.Message;
import mayo.networking.packet.SendTerrain;

import java.util.ArrayList;

public final class PacketRegistry {

    public static void register(Kryo kryo) {
        //types
        kryo.register(ArrayList.class);
        TerrainRegistry.register(kryo);

        //packets
        kryo.register(Handshake.class);
        kryo.register(Message.class);
        kryo.register(Login.class);
        kryo.register(SendTerrain.class);
    }
}
