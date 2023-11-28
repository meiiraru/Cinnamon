package mayo.networking.packet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class Message implements Packet {

    private final String owner;
    private String msg;

    public Message() {
        this.owner = mayo.Client.PLAYERNAME;
    }

    public Message msg(String msg) {
        this.msg = msg;
        return this;
    }

    @Override
    public void clientReceived(Client client, Connection connection) {
        System.out.printf("<%s> %s\n", owner, msg);
    }

    @Override
    public void serverReceived(Server server, Connection connection) {
        System.out.printf("[Server] <%s> %s\n", owner, msg);
        server.sendToAllExceptTCP(connection.getID(), this);
    }
}
