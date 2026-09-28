package cinnamon.settings;

import cinnamon.utils.Version;
import org.joml.Math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgsOptions {

    //central registry for all options
    private static final List<CliOption<?>> OPTIONS = new ArrayList<>();
    public static String cliArgs = "";

    /**
     * Registers a generic command line option
     */
    public static <T> CliOption<T> register(CliOption<T> option) {
        OPTIONS.add(option);
        return option;
    }

    //special case for help and version
    public static final CliOption<Boolean>
            HELP    = register(CliOption.flagOption("help", "Displays this help message", "-h", "--help")),
            VERSION = register(CliOption.flagOption("version", "Displays the current version", "-v", "--version"));

    //general options
    public static final CliOption<String>
            WORKING_DIR    = register(CliOption.stringOption("working_dir", "Sets the root working directory for the engine", "./", "-d", "--working-dir")),
            LOGGER_LEVEL   = register(CliOption.stringOption("logger_level", "Sets the default lowest logging level", "INFO", "-l", "--logger-level")),
            LOGGER_PATTERN = register(CliOption.stringOption("logger_pattern", "Defines the string pattern for the log messages", "%6$s[%1$tT] [%2$s/%3$s] (%4$s) %5$s%7$s", "--logger-pattern"));

    //graphics
    public static final CliOption<Boolean>
            FORCE_DISABLE_XR = register(CliOption.flagOption("force_disable_xr", "Forces the engine to disable XR support", "--force-disable-xr"));
    public static final CliOption<String>
            FORCE_GLFW_PLATFORM = register(CliOption.stringOption("force_glfw_platform", "Force the engine to use a specific GLFW platform", "", "--force-glfw-platform"));

    //other
    public static final CliOption<String>
            RENDER_DOC = register(CliOption.stringOption("render_doc", "Path to a RenderDoc library to be injected during rendering", "", "--render-doc"));
    public static final CliOption<Boolean>
            WINDOW_TITLE_FPS = register(CliOption.flagOption("window_title_fps", "Set the current FPS to display in the window title", "--window-title-fps"));

    /**
     * Finds the {@link CliOption} corresponding to a given command line flag
     * @param cliFlag The command line flag to search for
     * @return The corresponding {@link CliOption} if found, otherwise null
     */
    public static CliOption<?> getByCLIFlag(String cliFlag) {
        for (CliOption<?> option : OPTIONS) {
            for (String optionAlias : option.getCliFlags()) {
                if (optionAlias.equals(cliFlag))
                    return option;
            }
        }
        return null;
    }

    /**
     * Finds the {@link CliOption} corresponding to a given option name
     * @param name The name of the option to search for
     * @return The corresponding {@link CliOption} if found, otherwise null
     */
    public static CliOption<?> getByName(String name) {
        for (CliOption<?> option : OPTIONS) {
            if (option.getName().equals(name))
                return option;
        }
        return null;
    }

    private static void warn(String message) {
        System.err.println("Warning: " + message);
    }

    private static void error(String message) {
        throw new IllegalArgumentException(message);
    }

    /**
     * Parses the provided command line arguments and sets the corresponding {@link CliOption} values<br>
     * If an option is not provided, its default value will be used<br>
     * If an unknown option is provided, a warning will be printed<br>
     * If an option is provided but fails to parse, an error will be thrown<br>
     * If an option is provided but missing required arguments, an error will be thrown<br>
     * Built-in options like HELP and VERSION will be processed after parsing all arguments
     * @param args The command line arguments to parse
     */
    public static void parse(String... args) {
        cliArgs = String.join(" ", args);

        CliOption<?> currentOption = null;
        int argsRemaining = 0;

        String[] currentOptionArgs = null;
        for (String arg : args) {
            //if we are currently collecting arguments for an option
            if (argsRemaining > 0) {
                currentOptionArgs[currentOption.getArgCount() - argsRemaining] = arg;
                argsRemaining--;

                //once collected all required args, attempt to parse them
                if (argsRemaining == 0) {
                    try {
                        currentOption.set(currentOptionArgs);
                    } catch (Exception e) {
                        error("Failed to parse arguments for option " + currentOption.getName() + ": " + e.getMessage());
                    }
                    currentOption = null;
                    currentOptionArgs = null;
                }
            }

            //check for new option flag
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

                //apply the found args
                boolean packed = argsFound.length > 1;
                for (String alias : argsFound) {
                    CliOption<?> option = getByCLIFlag(alias);
                    if (option == null) {
                        warn("Unknown command line option: " + alias);
                    } else if (packed && option.getArgCount() > 0) {
                        error("Option " + alias + " requires arguments and cannot be used in packed form");
                    } else if (option.getArgCount() == 0) {
                        option.set(new String[0]); //option takes 0 arguments (like a boolean flag), parse immediately
                    } else {
                        //option needs arguments, prep the tracker variables
                        currentOption = option;
                        argsRemaining = option.getArgCount();
                        currentOptionArgs = new String[argsRemaining];
                    }
                }
            }

            //could not find option
            else {
                warn("Unexpected command line argument: " + arg);
            }
        }

        //if iteration ended but an option was still expecting arguments
        if (argsRemaining > 0)
            error("Missing " + argsRemaining + " arguments for option " + currentOption.getName());

        //process final built-in flags
        if (VERSION.get())
            System.out.println("Cinnamon version " + Version.CLIENT_VERSION);
        if (HELP.get())
            printHelp();
    }

    private static void printHelp() {
        //title
        System.out.println("Command Line Options:");

        //calculate max widths for column alignment
        int maxNameLen = 4;
        int maxDescLen = 11;
        int maxFlagsLen = 7;

        for (CliOption<?> option : OPTIONS) {
            maxNameLen = Math.max(maxNameLen, option.getName().length());
            maxDescLen = Math.max(maxDescLen, option.getDescription().length());
            String flags = String.join(", ", option.getCliFlags());
            maxFlagsLen = Math.max(maxFlagsLen, flags.length());
        }

        //build the format string dynamically
        //the negative sign (-) in %-##s means "left-justify"
        String format = "  %-" + (maxNameLen + 2) + "s   %-" + (maxDescLen + 2) + "s   %-" + (maxFlagsLen + 2) + "s   %s%n";

        //print the header
        System.out.printf(format, "NAME", "DESCRIPTION", "CLI FLAGS", "DEFAULT");

        //dividing line
        int totalWidth = maxNameLen + maxDescLen + maxFlagsLen + 35;
        System.out.println("-".repeat(Math.max(totalWidth, 75)));

        //settings rows
        for (CliOption<?> option : OPTIONS.stream().sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).toList()) {
            String flags = String.join(", ", option.getCliFlags());
            Object defaultValue = option.getDefaultValue();

            //format default value
            String def = defaultValue instanceof Object[] arr ? Arrays.toString(arr) : (defaultValue == null ? "false" : String.valueOf(defaultValue));
            if (def.isEmpty())
                def = "\"\"";

            //print the setting
            System.out.printf(format, option.getName(), option.getDescription(), flags, def);
        }
    }
}
