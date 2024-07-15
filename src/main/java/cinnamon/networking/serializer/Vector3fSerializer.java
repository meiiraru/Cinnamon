package cinnamon.networking.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.joml.Vector3f;

public class Vector3fSerializer extends Serializer<Vector3f> {

    public Vector3fSerializer() {
        super(true);
    }

    @Override
    public void write(Kryo kryo, Output output, Vector3f vec) {
        output.writeFloat(vec.x);
        output.writeFloat(vec.y);
        output.writeFloat(vec.z);
    }

    @Override
    public Vector3f read(Kryo kryo, Input input, Class<? extends Vector3f> type) {
        return new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
    }
}
