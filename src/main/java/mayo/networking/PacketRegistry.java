package mayo.networking;

import com.esotericsoftware.kryo.Kryo;
import mayo.networking.packet.*;
import mayo.networking.serializer.*;
import mayo.registry.*;
import mayo.world.entity.Entity;
import mayo.world.terrain.Terrain;

import java.util.ArrayList;

public final class PacketRegistry {

    public static void register(Kryo kryo) {
        kryo.setRegistrationRequired(false);
        kryo.setWarnUnregisteredClasses(true);

        // -- types -- //

        //misc
        kryo.register(ArrayList.class);

        //terrain
        kryo.addDefaultSerializer(Terrain.class, TerrainSerializer.class);
        Registry.registerType(kryo, TerrainRegistry.class);

        //entity
        kryo.addDefaultSerializer(Entity.class, EntitySerializer.class);
        Registry.registerType(kryo, EntityRegistry.class);

        // -- packets -- //

        kryo.register(Handshake.class);
        kryo.register(Message.class);
        kryo.register(Login.class);
        kryo.register(SendTerrain.class);
        kryo.register(SendEntities.class);
    }
}
