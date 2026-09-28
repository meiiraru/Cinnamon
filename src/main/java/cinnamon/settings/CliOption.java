package cinnamon.settings;

import java.util.function.Function;

/**
 * A class representing a command line option with a specific type
 * @param <T> The type of the option value
 */
public class CliOption<T> {

    private final String name;
    private final String description;
    private final String[] cliFlags;
    private final int argCount;
    private final T defaultValue;
    private T value;
    private final Function<String[], T> parser;

    /**
     * @param name The name of the option
     * @param description The description of the option
     * @param defaultValue The default value if not provided
     * @param argCount How many string arguments this option consumes
     * @param parser Function to convert the collected string arguments into type T
     * @param cliFlags The command line flags with dashes
     */
    public CliOption(String name, String description, T defaultValue, int argCount, Function<String[], T> parser, String... cliFlags) {
        this.name = name;
        this.description = description;
        this.cliFlags = cliFlags;
        this.argCount = argCount;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.parser = parser;
    }

    /**
     * Creates a boolean flag option that does not consume any arguments<br>
     * If the flag is present, the value will be true
     * @param name The name of the option
     * @param description The description of the option
     * @param cliFlags The command line flags with dashes
     * @return A {@link CliOption} representing the flag
     */
    public static CliOption<Boolean> flagOption(String name, String description, String... cliFlags) {
        return new CliOption<>(name, description, false, 0, _ -> true, cliFlags);
    }

    /**
     * Creates a string option that consumes one argument
     * @param name The name of the option
     * @param description The description of the option
     * @param defaultValue The default value if not provided
     * @param cliFlags The command line flags with dashes
     * @return A {@link CliOption} representing the string option
     */
    public static CliOption<String> stringOption(String name, String description, String defaultValue, String... cliFlags) {
        return new CliOption<>(name, description, defaultValue, 1, args -> args[0], cliFlags);
    }

    /**
     * Creates an integer option that consumes one String argument
     * @param name The name of the option
     * @param description The description of the option
     * @param defaultValue The default value if not provided
     * @param cliFlags The command line flags with dashes
     * @return A {@link CliOption} representing the integer option
     */
    public static CliOption<Integer> integerOption(String name, String description, int defaultValue, String... cliFlags) {
        return new CliOption<>(name, description, defaultValue, 1, args -> Integer.parseInt(args[0]), cliFlags);
    }

    /**
     * Creates a float option that consumes one String argument
     * @param name The name of the option
     * @param description The description of the option
     * @param defaultValue The default value if not provided
     * @param cliFlags The command line flags with dashes
     * @return A {@link CliOption} representing the float option
     */
    public static CliOption<Float> floatOption(String name, String description, float defaultValue, String... cliFlags) {
        return new CliOption<>(name, description, defaultValue, 1, args -> Float.parseFloat(args[0]), cliFlags);
    }

    /**
     * @return The name of the option
     */
    public String getName() {
        return name;
    }

    /**
     * @return The description of the option
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The command line flags with dashes
     */
    public String[] getCliFlags() {
        return cliFlags;
    }

    /**
     * @return How many arguments this option consumes
     */
    public int getArgCount() {
        return argCount;
    }

    /**
     * @return The default value of this option
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return The current value of this option
     */
    public T get() {
        return value;
    }

    /**
     * Sets the value of this option by parsing the provided arguments
     * @param args The arguments to parse into the option value
     */
    public void set(String[] args) {
        this.value = parser.apply(args);
    }
}
