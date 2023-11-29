package mayo.registry;

import com.esotericsoftware.kryo.Kryo;

import java.util.function.Supplier;

public interface Registry<T> {
    Supplier<T> getFactory();
    void register(Kryo kryo);

    static <T extends Enum<T> & Registry<?>> void registerType(Kryo kryo, Class<T> registry) {
        kryo.register(registry);

        for (T enumEntry : registry.getEnumConstants())
            enumEntry.register(kryo);
    }
}
