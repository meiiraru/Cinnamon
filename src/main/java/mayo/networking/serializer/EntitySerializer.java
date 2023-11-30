package mayo.networking.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mayo.registry.EntityRegistry;
import mayo.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.UUID;

public class EntitySerializer extends Serializer<Entity> {
    @Override
    public void write(Kryo kryo, Output output, Entity entity) {
        //uuid
        kryo.writeObject(output, entity.getUUID());

        //type
        kryo.writeObject(output, entity.getType());

        //pos
        Vector3f pos = entity.getPos();
        output.writeFloat(pos.x);
        output.writeFloat(pos.y);
        output.writeFloat(pos.z);

        //rotation
        Vector2f rot = entity.getRot();
        output.writeFloat(rot.x);
        output.writeFloat(rot.y);
    }

    @Override
    public Entity read(Kryo kryo, Input input, Class<? extends Entity> type) {
        //uuid
        UUID uuid = kryo.readObject(input, UUID.class);

        //type
        EntityRegistry entityType = kryo.readObject(input, EntityRegistry.class);
        Entity entity = entityType.getFactory().apply(uuid);

        //pos
        entity.setPos(input.readFloat(), input.readFloat(), input.readFloat());

        //rotation
        entity.setRot(input.readFloat(), input.readFloat());

        //add
        return entity;
    }
}
