package cinnamon.networking.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import cinnamon.world.entity.living.LivingEntity;

public class LivingEntitySerializer extends Serializer<LivingEntity> {
    @Override
    public void write(Kryo kryo, Output output, LivingEntity entity) {
        //write entity
        kryo.writeObject(output, entity, new EntitySerializer());

        //write health
        output.writeInt(entity.getMaxHealth());
        output.writeInt(entity.getHealth());

        //selected index
        output.writeVarInt(entity.getInventory().getSelectedIndex(), true);
    }

    @Override
    public LivingEntity read(Kryo kryo, Input input, Class<? extends LivingEntity> type) {
        //read entity
        LivingEntity entity = kryo.readObject(input, type, new EntitySerializer());

        //read health
        entity.setMaxHealth(input.readInt());
        entity.setHealth(input.readInt());

        //selected index
        entity.getInventory().setSelectedIndex(input.readVarInt(true));

        //return
        return entity;
    }
}
