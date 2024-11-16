package cinnamon.settings;

import com.google.gson.JsonElement;
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
            super(name, Math.clamp(defaultValue, min, max));
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Float value) {
            super.set(Math.clamp(value, min, max));
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
}
