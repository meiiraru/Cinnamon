package mayo.networking.registry;

import com.esotericsoftware.kryo.Kryo;
import mayo.networking.packet.Handshake;
import mayo.networking.packet.Login;
import mayo.networking.packet.Message;
import mayo.networking.packet.SendTerrain;
import mayo.networking.serializer.TerrainSerializer;
import mayo.registry.Registry;
import mayo.registry.TerrainRegistry;
import mayo.world.terrain.Terrain;

import java.util.ArrayList;

public final class PacketRegistry {

    public static void register(Kryo kryo) {
        //serializers
        kryo.addDefaultSerializer(Terrain.class, TerrainSerializer.class);

        //types
        kryo.register(ArrayList.class);
        Registry.registerType(kryo, TerrainRegistry.class);

        //packets
        kryo.register(Handshake.class);
        kryo.register(Message.class);
        kryo.register(Login.class);
        kryo.register(SendTerrain.class);
    }
}
