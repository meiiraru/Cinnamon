package cinnamon.networking.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

public class TerrainSerializer extends Serializer<Terrain> {

    public TerrainSerializer() {
        super(true);
    }

    @Override
    public void write(Kryo kryo, Output output, Terrain terrain) {
        //type
        kryo.writeObject(output, terrain.getType());

        //pos
        Vector3f pos = terrain.getPos();
        output.writeFloat(pos.x);
        output.writeFloat(pos.y);
        output.writeFloat(pos.z);

        //rotation
        output.writeByte(terrain.getRotation());
    }

    @Override
    public Terrain read(Kryo kryo, Input input, Class<? extends Terrain> type) {
        //type
        TerrainRegistry terrainType = kryo.readObject(input, TerrainRegistry.class);
        Terrain terrain = terrainType.getFactory().get();

        //pos
        terrain.setPos(input.readFloat(), input.readFloat(), input.readFloat());

        //rotation
        terrain.setRotation(input.readByte());

        //add
        return terrain;
    }
}
