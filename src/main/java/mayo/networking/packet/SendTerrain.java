package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import mayo.world.WorldClient;
import mayo.world.terrain.Terrain;

import java.util.ArrayList;
import java.util.Collection;

public class SendTerrain implements Packet {

    private Collection<Terrain> terrain;

    public SendTerrain terrain(Collection<Terrain> terrain) {
        this.terrain = new ArrayList<>(terrain);
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        WorldClient world = mayo.Client.getInstance().world;
        //for (Terrain t : terrain)
        //    world.addTerrain(t);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {}
}
