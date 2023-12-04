package mayo.networking.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mayo.world.entity.living.Player;

public class PlayerEntitySerializer extends Serializer<Player> {

    @Override
    public void write(Kryo kryo, Output output, Player player) {
        //write entity
        kryo.writeObject(output, player, new LivingEntitySerializer());

        //write name
        output.writeString(player.getName());
    }

    @Override
    public Player read(Kryo kryo, Input input, Class<? extends Player> type) {
        //read entity
        Player player = kryo.readObject(input, type, new LivingEntitySerializer());

        //read name
        player.setName(input.readString());

        return player;
    }
}
