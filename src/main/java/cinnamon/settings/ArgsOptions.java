package cinnamon.settings;

import cinnamon.utils.Version;
import org.joml.Math;

import java.util.Arrays;
import java.util.function.Predicate;

public enum ArgsOptions {
    //special case for help and version
    HELP(null, "-h", "--help"),
    VERSION(null, "-v", "--version"),

    //general options
    WORKING_DIR("./", "-d", "--working-dir"),
    LOGGER_LEVEL("INFO", "-l", "--logger-level"),
    LOGGER_PATTERN("[%1$tT] [%2$s/%3$s] (%4$s) %5$s", "--logger-pattern"),

    //graphics
    EXPERIMENTAL_OPENGL_ES(null, "--experimental-opengl-es"),
    FORCE_DISABLE_XR(null, "--force-disable-xr"),

    //other
    PLAYERNAME("Player%03d".formatted((int) (Math.random() * 456) + 1), "--player-name"),
    RENDER_DOC("", "--render-doc");

    private final String[] aliases;
    private final int argCount;
    private final Predicate<String> predicate;
    private final Object defaultValue;
    private Object value;

    ArgsOptions(Object defaultValue, String... aliases) {
        this(defaultValue, obj -> true, aliases);
    }

    ArgsOptions(Object defaultValue, Predicate<String> predicate, String... aliases) {
        this.aliases = aliases;
        this.argCount = defaultValue == null ? 0 : (defaultValue instanceof Object[] arr ? arr.length : 1);
        this.predicate = predicate;
        this.defaultValue = defaultValue;
        this.value = defaultValue instanceof Object[] arr ? Arrays.copyOf(arr, arr.length) : defaultValue;
    }

    public boolean getAsBool() {
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean getAsBool(int i) {
        return Boolean.parseBoolean(String.valueOf(((Object[]) value)[i]));
    }

    public int getAsInt() {
        return Integer.parseInt(String.valueOf(value));
    }

    public int getAsInt(int i) {
        return Integer.parseInt(String.valueOf(((Object[]) value)[i]));
    }

    public float getAsFloat() {
        return Float.parseFloat(String.valueOf(value));
    }

    public float getAsFloat(int i) {
        return Float.parseFloat(String.valueOf(((Object[]) value)[i]));
    }

    public String getAsString() {
        return String.valueOf(value);
    }

    public String getAsString(int i) {
        return String.valueOf(((Object[]) value)[i]);
    }

    public static ArgsOptions forAlias(String alias) {
        for (ArgsOptions option : ArgsOptions.values())
            for (String optionAlias : option.aliases)
                if (optionAlias.equals(alias))
                    return option;

        return null;
    }

    private static void error(String message) {
        throw new IllegalArgumentException(message);
    }

    public static void parse(String... args) {
        ArgsOptions currentOption = null;
        int argsRemaining = 0;

        for (String arg : args) {
            if (argsRemaining > 0) {
                if (!currentOption.predicate.test(arg)) {
                    error("Invalid argument for option " + currentOption.name() + ": " + arg);
                } else {
                    if (currentOption.argCount == 1) {
                        currentOption.value = arg;
                    } else {
                        ((Object[]) currentOption.value)[currentOption.argCount - argsRemaining] = arg;
                    }
                }

                argsRemaining--;
                if (argsRemaining == 0)
                    currentOption = null;
            }

            //check for arg
            else if (arg.startsWith("-")) {
                String[] argsFound;

                //check long form
                if (arg.startsWith("--")) {
                    argsFound = new String[]{arg};
                }
                //packed short form
                else {
                    argsFound = new String[arg.length() - 1];
                    for (int i = 1; i < arg.length(); i++)
                        argsFound[i - 1] = "-" + arg.charAt(i);
                }

                //apply the found aliases
                boolean packed = argsFound.length > 1;
                for (String alias : argsFound) {
                    ArgsOptions option = forAlias(alias);
                    if (option == null) {
                        error("Unknown command line option: " + alias);
                    } else if (packed && option.argCount > 0) {
                        error("Option " + alias + " requires arguments and cannot be used in packed form");
                    } else if (option.argCount == 0) {
                        option.value = "true";
                    } else {
                        currentOption = option;
                        argsRemaining = option.argCount;
                    }
                }
            }

            //could not find option
            else error("Unexpected command line argument: " + arg);
        }

        if (argsRemaining > 0)
            error("Missing " + argsRemaining + " arguments for option " + currentOption.name());

        if (VERSION.getAsBool())
            System.out.println("Cinnamon version " + Version.CLIENT_VERSION);

        if (HELP.getAsBool()) {
            System.out.println("Command Line Options:");
            for (ArgsOptions option : ArgsOptions.values()) {
                String aliases = String.join(", ", option.aliases);
                String def = option.defaultValue instanceof Object[] arr ? Arrays.toString(arr) : option.defaultValue == null ? "false" : String.valueOf(option.defaultValue);
                String spacing1 = " ".repeat(Math.max(24 - option.name().length(), 1));
                String spacing2 = " ".repeat(Math.max(24 - aliases.length(), 1));

                System.out.println(option.name() + spacing1 + aliases + spacing2 + "Default " + def);
            }
        }
    }
}
