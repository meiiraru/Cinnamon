package mayo.registry;

import com.esotericsoftware.kryo.Kryo;

public interface Registry {

    void register(Kryo kryo);

    static <T extends Enum<T> & Registry> void registerType(Kryo kryo, Class<T> registry) {
        kryo.register(registry);

        for (T enumEntry : registry.getEnumConstants())
            enumEntry.register(kryo);
    }
}
