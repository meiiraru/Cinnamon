package mayo.world;

import mayo.networking.ServerConnection;
import mayo.networking.packet.RemoveEntity;
import mayo.registry.LivingModelRegistry;
import mayo.utils.Resource;
import mayo.world.entity.Entity;
import mayo.world.entity.living.Player;
import mayo.world.entity.vehicle.Cart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldServer extends World {

    private final Map<Integer, Player> players = new HashMap<>();

    @Override
    public void init() {
        //load level
        LevelLoad.load(this, new Resource("data/levels/level0.json"));

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);

        Cart c = new Cart(UUID.randomUUID());
        c.setPos(10, 2, 10);
        this.addEntity(c);

        Cart c2 = new Cart(UUID.randomUUID());
        c2.setPos(15, 2, 10);
        this.addEntity(c2);

        runScheduledTicks();
    }

    @Override
    public void close() {
    }

    @Override
    public void entityRemoved(UUID uuid) {
        ServerConnection.connection.sendToAllTCP(new RemoveEntity().uuid(uuid));
    }

    public Map<UUID, Entity> getEntities() {
        return this.entities;
    }

    public Player addPlayer(int internalID, String name, UUID uuid) {
        Player player = new Player(name, uuid, LivingModelRegistry.STRAWBERRY);

        this.addEntity(player);
        players.put(internalID, player);

        return player;
    }

    public void removePlayer(int internalID) {
        Player player = players.remove(internalID);
        if (player != null) player.remove();
    }

    public Player getPlayerByID(int internalID) {
        return players.get(internalID);
    }
}
