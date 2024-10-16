package cinnamon.networking;

/*
import com.esotericsoftware.kryo.Kryo;
import cinnamon.networking.packet.*;
import cinnamon.networking.serializer.*;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.UUID;

public final class PacketRegistry {

    public static void register(Kryo kryo) {
        //kryo.setRegistrationRequired(false);
        //kryo.setWarnUnregisteredClasses(true);

        // -- types -- //

        //misc
        kryo.register(ArrayList.class);
        kryo.register(UUID.class, new UUIDSerializer());
        kryo.register(Vector2f.class, new Vector2fSerializer());
        kryo.register(Vector3f.class, new Vector3fSerializer());

        //terrain
        kryo.addDefaultSerializer(Terrain.class, TerrainSerializer.class);
        //Registry.registerType(kryo, TerrainRegistry.class);

        //entity
        kryo.addDefaultSerializer(Entity.class, EntitySerializer.class);
        kryo.addDefaultSerializer(Projectile.class, ProjectileSerializer.class);
        kryo.addDefaultSerializer(LivingEntity.class, LivingEntitySerializer.class);
        kryo.addDefaultSerializer(Player.class, PlayerEntitySerializer.class);
        //Registry.registerType(kryo, EntityRegistry.class);

        // -- packets -- //

        kryo.register(Handshake.class);
        kryo.register(Message.class);
        kryo.register(Login.class);
        kryo.register(SendTerrain.class);
        kryo.register(SendEntities.class);
        kryo.register(AddEntity.class);
        kryo.register(RemoveEntity.class);
        kryo.register(ClientMovement.class);
        kryo.register(EntitySync.class);
        kryo.register(ClientEntityAction.class);
        kryo.register(Respawn.class);
        kryo.register(SelectItem.class);
        kryo.register(SyncHealth.class);
    }
}
 */