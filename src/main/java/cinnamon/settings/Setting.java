package cinnamon.settings;

import cinnamon.input.Keybind.KeyType;
import cinnamon.utils.Maths;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.function.Consumer;

public abstract class Setting<T> {

    private final String name;
    private final T defaultValue;
    private T value;

    private Consumer<T> consumer;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        Settings.SETTINGS.add(this);
    }

    public abstract void fromJson(JsonElement element);

    public abstract JsonElement toJson();

    public String getName() {
        return name;
    }

    public T getDefault() {
        return defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
        if (consumer != null)
            consumer.accept(value);
    }

    public void setListener(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public String toString() {
        return name + " = " + value;
    }

    // -- settings types -- //


    public static class Ints extends Setting<Integer> {
        public Ints(String name, Integer defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsInt());
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(get());
        }
    }

    public static class Floats extends Setting<Float> {
        public Floats(String name, Float defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsFloat());
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(get());
        }
    }

    public static class Strings extends Setting<String> {
        public Strings(String name, String defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsString());
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(get());
        }
    }

    public static class Bools extends Setting<Boolean> {
        public Bools(String name, Boolean defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsBoolean());
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(get());
        }
    }

    public static class Enums<T extends Enum<T>> extends Setting<T> {
        public Enums(String name, T defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(T.valueOf(getDefault().getDeclaringClass(), element.getAsString().toUpperCase()));
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(get().name().toLowerCase());
        }
    }

    public static class Ranges extends Setting<Float> {
        private final float min;
        private final float max;

        public Ranges(String name, Float defaultValue, float min, float max) {
            super(name, Maths.clamp(defaultValue, min, max));
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Float value) {
            super.set(Maths.clamp(value, min, max));
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsFloat());
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }
    }

    public static class Keybind extends Setting<cinnamon.input.Keybind> {
        public Keybind(String name, int key, KeyType type) {
            this(name, key, 0, type);
        }

        public Keybind(String name, int key, int mods, KeyType type) {
            super(name, new cinnamon.input.Keybind(name, key, mods, type));
        }

        @Override
        public void fromJson(JsonElement element) {
            JsonObject obj = element.getAsJsonObject();
            int key = obj.get("key").getAsInt();
            int mods = obj.get("mods").getAsInt();
            String type = obj.get("type").getAsString();
            get().set(key, mods, KeyType.valueOf(type.toUpperCase()));
        }

        @Override
        public JsonElement toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", get().getKey());
            obj.addProperty("mods", get().getMods());
            obj.addProperty("type", get().getType().name().toLowerCase());
            return obj;
        }
    }
}
