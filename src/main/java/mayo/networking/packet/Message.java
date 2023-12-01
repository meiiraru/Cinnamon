package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class Message extends PacketWithOwner {

    private String msg;

    public Message msg(String msg) {
        this.msg = msg;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        System.out.println(msg);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {
        String s = "<%s> %s".formatted(name, msg);
        System.out.println("[Server] " + s);
        server.sendToAllTCP(new Message().msg(s));
    }
}
