package cinnamon.settings;

import cinnamon.input.Keybind.KeyType;
import cinnamon.math.Maths;
import cinnamon.utils.Pair;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Setting<T> {

    private final String name;
    private final T defaultValue;
    private T value, tempValue;

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

    public T getTempValue() {
        return tempValue;
    }

    public void set(T value) {
        this.value = value;
        if (consumer != null)
            consumer.accept(value);
    }

    public void setTempValue(T tempValue) {
        this.tempValue = tempValue;
    }

    public void applyTemp() {
        if (tempValue != null) {
            set(tempValue);
            tempValue = null;
        }
    }

    public void discardTemp() {
        tempValue = null;
    }

    public void setListener(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    public boolean isDefault() {
        return value.equals(defaultValue);
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }


    // -- types -- //


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

    public static class Ranges extends Setting<Float> {
        private final float min;
        private final float max;
        private final float step;

        public Ranges(String name, Float defaultValue, float min, float max) {
            this(name, defaultValue, min, max, 0.01f);
        }

        public Ranges(String name, Float defaultValue, float min, float max, float step) {
            super(name, Maths.clamp(defaultValue, min, max));
            this.min = min;
            this.max = max;
            this.step = step;
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

        public float getStep() {
            return step;
        }
    }

    public static class IntRanges extends Setting<Integer> {
        private final int min;
        private final int max;
        private final int step;

        public IntRanges(String name, Integer defaultValue, int min, int max) {
            this(name, defaultValue, min, max, 1);
        }

        public IntRanges(String name, Integer defaultValue, int min, int max, int step) {
            super(name, Maths.clamp(defaultValue, min, max));
            this.min = min;
            this.max = max;
            this.step = step;
        }

        @Override
        public void set(Integer value) {
            super.set(Maths.clamp(value, min, max));
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsInt());
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public int getStep() {
            return step;
        }
    }

    public static class Enums extends Strings {
        private final Class<? extends Enum<?>> enumClass;

        public Enums(String name, String defaultValue, Class<? extends Enum<?>> enumClass) {
            super(name, defaultValue);
            this.enumClass = enumClass;
        }

        @Override
        public void fromJson(JsonElement element) {
            String val = element.getAsString();
            for (Enum<?> e : enumClass.getEnumConstants()) {
                if (e.name().equals(val)) {
                    set(val);
                    return;
                }
            }
            throw new IllegalArgumentException("Invalid enum value: " + val + " for enum class: " + enumClass.getName());
        }

        public Class<? extends Enum<?>> getEnumClass() {
            return enumClass;
        }
    }

    public static class List extends Strings {
        private final Supplier<java.util.List<Pair<String, String>>> valuesSupplier;

        public List(String name, String defaultValue, Supplier<java.util.List<Pair<String, String>>> valuesSupplier) {
            super(name, defaultValue);
            this.valuesSupplier = valuesSupplier;
        }

        public Supplier<java.util.List<Pair<String, String>>> getValuesSupplier() {
            return valuesSupplier;
        }
    }

    public static class Keybind extends Setting<cinnamon.input.Keybind> {
        //defaults
        private final int key, mods, joystick;
        private final KeyType type;

        //temp
        private int tempKey, tempMods, tempJoystick;
        private KeyType tempType;

        public Keybind(String name, int key, KeyType type) {
            this(name, key, 0, type);
        }

        public Keybind(String name, int key, int mods, KeyType type) {
            this(name, key, mods, type, -1);
        }

        public Keybind(String name, int key, int mods, KeyType type, int joystick) {
            super(name, new cinnamon.input.Keybind(name, key, mods, type, joystick));
            this.key = key;
            this.mods = mods;
            this.type = type;
            this.joystick = joystick;
        }

        @Override
        public void fromJson(JsonElement element) {
            JsonObject obj = element.getAsJsonObject();
            int key = obj.get("key").getAsInt();
            int mods = obj.get("mods").getAsInt();
            String type = obj.get("type").getAsString();
            int joystick = obj.get("joystick").getAsInt();
            get().set(key, mods, KeyType.valueOf(type.toUpperCase()), joystick);
        }

        @Override
        public JsonElement toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", get().getKey());
            obj.addProperty("mods", get().getMods());
            obj.addProperty("type", get().getType().name().toLowerCase());
            obj.addProperty("joystick", get().getJoystick());
            return obj;
        }

        @Override
        public boolean isDefault() {
            cinnamon.input.Keybind key = get();
            return key.getKey() == this.key && key.getMods() == this.mods && key.getType() == this.type && key.getJoystick() == this.joystick;
        }

        @Override
        public void applyTemp() {
            super.applyTemp();
            if (tempType != null)
                get().set(tempKey, tempMods, tempType, tempJoystick);
        }

        @Override
        public void discardTemp() {
            super.discardTemp();
            setTemp(0, 0, null, 0);
        }

        public void setTemp(int key, int mods, KeyType type, int joystick) {
            this.tempKey = key;
            this.tempMods = mods;
            this.tempType = type;
            this.tempJoystick = joystick;
        }

        public int getDefaultKey() {
            return key;
        }

        public int getDefaultMods() {
            return mods;
        }

        public KeyType getDefaultType() {
            return type;
        }

        public int getDefaultJoystick() {
            return joystick;
        }

        public int getTempKey() {
            return tempKey;
        }

        public int getTempMods() {
            return tempMods;
        }

        public KeyType getTempType() {
            return tempType;
        }

        public int getTempJoystick() {
            return tempJoystick;
        }
    }
}
