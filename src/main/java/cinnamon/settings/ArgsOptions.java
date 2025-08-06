package cinnamon.settings;

import cinnamon.utils.Version;

public enum ArgsOptions {
    //special case for help and version
    HELP(null, "-h", "--help"),
    VERSION(null, "-v", "--version"),

    //general options
    WORKING_DIR("./", "-d", "--working-dir"),
    LOGGER_LEVEL("INFO", "-l", "--logger-level"),
    LOGGER_PATTERN("[%1$tT] [%2$s/%3$s] (%4$s) %5$s", "--logger-pattern"),

    //other
    PLAYERNAME("Player%03d".formatted((int) (Math.random() * 456) + 1), "--player-name"),
    FORCE_DISABLE_XR(null, "--force-disable-xr"),
    EXPERIMENTAL_OPENGL_ES(null, "--experimental-opengl-es");

    private final String[] aliases;
    private final String defaultValue;
    private String value;

    ArgsOptions(Object defaultValue, String... aliases) {
        this.defaultValue = this.value = defaultValue == null ? null : defaultValue.toString();
        this.aliases = aliases;
    }

    public boolean getAsBool() {
        return value != null;
    }

    public int getAsInt() {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return Integer.parseInt(defaultValue);
        }
    }

    public float getAsFloat() {
        try {
            return Float.parseFloat(value);
        } catch (Exception ignored) {
            return Float.parseFloat(defaultValue);
        }
    }

    public String getAsString() {
        return value.isBlank() ? defaultValue : value;
    }

    public static ArgsOptions forAlias(String alias) {
        for (ArgsOptions option : ArgsOptions.values())
            for (String optionAlias : option.aliases)
                if (optionAlias.equals(alias))
                    return option;

        return null;
    }

    public static void parse(String... args) {
        ArgsOptions currentOption = null;
        for (String arg : args) {
            //parse argument
            if (arg.startsWith("-")) {
                String[] argsFound; 

                //long form
                if (arg.startsWith("--")) {
                    argsFound = new String[]{arg};
                }
                //short form / packed
                else {
                    argsFound = new String[arg.length() - 1];
                    for (int i = 1; i < arg.length(); i++)
                        argsFound[i - 1] = "-" + arg.charAt(i);
                }

                //apply the found aliases
                for (String alias : argsFound) {
                    currentOption = forAlias(alias);
                    if (currentOption == null) {
                        System.err.println("Unknown command line option: " + alias);
                    } else if (currentOption.value == null) {
                        currentOption.value = "true";
                    }
                }
            }
            //apply argument value
            else if (currentOption != null) {
                currentOption.value = arg;
                currentOption = null;
            }
            //flow error
            else {
                System.err.println("Unexpected command line argument: " + arg);
            }
        }

        if (VERSION.getAsBool())
            System.out.println("Cinnamon version " + Version.CLIENT_VERSION);

        if (HELP.getAsBool()) {
            System.out.println("Command Line Options:");
            for (ArgsOptions option : ArgsOptions.values()) {
                String aliases = String.join(", ", option.aliases);
                String def = option.defaultValue == null ? "false" : option.defaultValue;
                String spacing1 = " ".repeat(Math.max(24 - option.name().length(), 1));
                String spacing2 = " ".repeat(Math.max(24 - aliases.length(), 1));

                System.out.println(option.name() + spacing1 + aliases + spacing2 + "Default " + def);
            }
        }
    }
}
