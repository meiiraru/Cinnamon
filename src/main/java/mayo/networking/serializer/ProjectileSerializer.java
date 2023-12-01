package mayo.networking.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mayo.world.entity.projectile.Projectile;

import java.util.UUID;

public class ProjectileSerializer extends Serializer<Projectile> {
    @Override
    public void write(Kryo kryo, Output output, Projectile projectile) {
        //projectile itself
        kryo.writeObject(output, projectile, new EntitySerializer());

        //owner uuid
        kryo.writeObject(output, projectile.getOwner());

        //fields
        output.writeInt(projectile.getDamage());
        output.writeBoolean(projectile.isCrit());
        output.writeInt(projectile.getLifetime());
    }

    @Override
    public Projectile read(Kryo kryo, Input input, Class<? extends Projectile> type) {
        //projectile
        Projectile projectile = kryo.readObject(input, Projectile.class, new EntitySerializer());

        //owner
        projectile.setOwner(kryo.readObject(input, UUID.class));

        //fields
        projectile.setDamage(input.readInt());
        projectile.setCrit(input.readBoolean());
        projectile.setLifetime(input.readInt());

        //return
        return projectile;
    }
}
