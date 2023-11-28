package mayo.world;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import mayo.networking.NetworkConstants;
import mayo.networking.PacketRegistry;
import mayo.networking.packet.Packet;
import mayo.utils.Resource;
import mayo.world.entity.vehicle.Cart;
import mayo.world.light.Light;
import mayo.world.light.Spotlight;

public class WorldServer extends World {

    private final Spotlight flashlight = (Spotlight) new Spotlight().cutOff(25f, 45f).brightness(64);
    private Server server;

    @Override
    public void init() {
        //load level
        LevelLoad.load(this, new Resource("data/levels/level0.json"));

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);
        //rip for-loop
        addLight(new Light().pos(-5.5f, 0.5f, 2f).color(0x000000));
        addLight(new Light().pos(-3.5f, 0.5f, 2f).color(0xFF0000));
        addLight(new Light().pos(-1.5f, 0.5f, 2f).color(0x00FF00));
        addLight(new Light().pos(0.5f, 0.5f, 2f).color(0x0000FF));
        addLight(new Light().pos(2.5f, 0.5f, 2f).color(0x00FFFF));
        addLight(new Light().pos(4.5f, 0.5f, 2f).color(0xFF00FF));
        addLight(new Light().pos(6.5f, 0.5f, 2f).color(0xFFFF00));
        addLight(new Light().pos(8.5f, 0.5f, 2f).color(0xFFFFFF));
        addLight(flashlight);

        Cart c = new Cart(this);
        c.setPos(10, 2, 10);
        this.addEntity(c);

        Cart c2 = new Cart(this);
        c2.setPos(15, 2, 10);
        this.addEntity(c2);
    }

    @Override
    public void close() {
        if (server != null)
            server.close();
    }

    public void openToLAN() {
        try {
            Server server = new Server();
            PacketRegistry.register(server.getKryo());
            server.start();
            server.bind(NetworkConstants.TCP_PORT, NetworkConstants.UDP_PORT);
            this.server = server;

            server.addListener(new Listener() {
                public void received (Connection connection, Object object) {
                    if (object instanceof Packet p)
                        p.serverReceived(server, connection);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
