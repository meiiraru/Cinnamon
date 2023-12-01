package mayo.networking.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.joml.Vector2f;

public class Vector2fSerializer extends Serializer<Vector2f> {

    public Vector2fSerializer() {
        super(true);
    }

    @Override
    public void write(Kryo kryo, Output output, Vector2f vec) {
        output.writeFloat(vec.x);
        output.writeFloat(vec.y);
    }

    @Override
    public Vector2f read(Kryo kryo, Input input, Class<? extends Vector2f> type) {
        return new Vector2f(input.readFloat(), input.readFloat());
    }
}
