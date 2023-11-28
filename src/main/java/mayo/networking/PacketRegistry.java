package mayo.networking;

import com.esotericsoftware.kryo.Kryo;
import mayo.networking.packet.*;

public final class PacketRegistry {

    public static void register(Kryo kryo) {
        kryo.register(Handshake.class);
        kryo.register(Message.class);
    }
}
