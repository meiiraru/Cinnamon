package mayo.networking.registry;

import com.esotericsoftware.kryo.Kryo;
import mayo.model.ModelRegistry;
import mayo.networking.serializer.TerrainSerializer;
import mayo.world.terrain.*;

public final class TerrainRegistry {

    public static void register(Kryo kryo) {
        //serializers
        kryo.addDefaultSerializer(Terrain.class, TerrainSerializer.class);

        //type enum
        kryo.register(ModelRegistry.Terrain.class);

        //terrain types
        kryo.register(Box.class);
        kryo.register(Fence.class);
        kryo.register(Grass.class);
        kryo.register(LightPole.class);
        kryo.register(Pillar.class);
        kryo.register(Sphere.class);
        kryo.register(Teapot.class);
        kryo.register(ToriiGate.class);
        kryo.register(Tree.class);
    }
}
