package cinnamon.settings;

import cinnamon.utils.Resource;
import com.google.gson.JsonElement;

public abstract class Setting<T> {

    private final String category;
    private final String name;
    private final T defaultValue;
    private T value;

    public Setting(String category, String name, T defaultValue) {
        this.category = category;
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        Settings.SETTINGS.add(this);
    }

    public abstract void fromJson(JsonElement element);

    public String getCategory() {
        return category;
    }

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
    }

    @Override
    public String toString() {
        return category + "." + name + " = " + value;
    }

    // -- settings types -- //


    public static class SInt extends Setting<Integer> {
        public SInt(String category, String name, Integer defaultValue) {
            super(category, name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsInt());
        }
    }

    public static class SFloat extends Setting<Float> {
        public SFloat(String category, String name, Float defaultValue) {
            super(category, name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsFloat());
        }
    }

    public static class SString extends Setting<String> {
        public SString(String category, String name, String defaultValue) {
            super(category, name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsString());
        }
    }

    public static class SBool extends Setting<Boolean> {
        public SBool(String category, String name, Boolean defaultValue) {
            super(category, name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(element.getAsBoolean());
        }
    }

    public static class SEnum<T extends Enum<T>> extends Setting<T> {
        public SEnum(String category, String name, T defaultValue) {
            super(category, name, defaultValue);
        }

        @Override
        public void fromJson(JsonElement element) {
            set(T.valueOf(getDefault().getDeclaringClass(), element.getAsString().toUpperCase()));
        }
    }

    public static class SRange extends Setting<Float> {
        private final float min;
        private final float max;

        public SRange(String category, String name, Float defaultValue, float min, float max) {
            super(category, name, defaultValue);
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
    }
}
