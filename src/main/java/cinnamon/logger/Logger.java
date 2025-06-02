package cinnamon.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Logger {

    private static final String ROOT_NAMESPACE = "cinnamon";
    private static final Logger ROOT_LOGGER = new Logger();

    private final String name;
    private final List<LogOutput> outputs = new ArrayList<>();

    private Logger() {
        this.name = ROOT_NAMESPACE;
    }

    public Logger(String name) {
        this.name = ROOT_LOGGER.name + "/" + name;
    }

    public static Logger getRootLogger() {
        return ROOT_LOGGER;
    }

    public void addOutput(LogOutput output) {
        outputs.add(output);
    }

    public void clearOutputs() {
        outputs.clear();
    }

    public void log(Level level, Object message, Object... args) {
        Throwable throwable;
        String msg;

        if (args == null || args.length == 0) {
            throwable = null;
            msg = message == null ? "" : message.toString();
        } else {
            throwable = args[args.length - 1] instanceof Throwable t ? t : null;
            Object[] formatArgs = throwable == null ? args : Arrays.copyOf(args, args.length - 1);
            msg = message == null ? "" : message.toString().formatted(formatArgs);
        }

        Calendar calendar = Calendar.getInstance();
        String threadName = Thread.currentThread().getName();

        if (this != ROOT_LOGGER)
            ROOT_LOGGER.log(calendar, threadName, level, name, msg, throwable);

        log(calendar, threadName, level, name, msg, throwable);
    }

    private void log(Calendar calendar, String threadName, Level level, String name, String msg, Throwable throwable) {
        for (LogOutput output : outputs) {
            if (output.shouldLog(level)) {
                String logMessage = output.applyFormatting(calendar, threadName, level, name, msg);
                output.write(logMessage, throwable);
            }
        }
    }

    public void trace(Object message, Object... args) {
        log(Level.TRACE, message, args);
    }

    public void debug(Object message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    public void info(Object message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void warn(Object message, Object... args) {
        log(Level.WARN, message, args);
    }

    public void error(Object message, Object... args) {
        log(Level.ERROR, message, args);
    }

    public void fatal(Object message, Object... args) {
        log(Level.FATAL, message, args);
    }
}