package cinnamon.logger;

import cinnamon.gui.DebugScreen;
import cinnamon.utils.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cinnamon.Client.LOGGER;

public class LoggerConfig {

    public static final Path LOG_OUTPUT = IOUtils.ROOT_FOLDER.resolve("logs/log.log");
    public static final String PATTERN = "[%1$tT] [%2$s/%3$s] (%4$s) %5$s\n";
    public static final Level DEFAULT_LEVEL = Level.INFO;

    public static void initialize() {
        //save old log file
        Exception gzipException = saveOldLog();

        //configure logger
        Logger root = Logger.getRootLogger();
        configureLogger(root);

        //system out/err redirection
        System.setOut(new LoggerStream(LOGGER::info));
        System.setErr(new LoggerStream(LOGGER::error));

        //debugLogLevels(root);

        //log any gzip exceptions
        if (gzipException != null)
            LOGGER.error("Failed to parse previous log file", gzipException);
    }

    private static void configureLogger(Logger logger) {
        //add console output
        ConsoleOutput consoleOutput = new ConsoleOutput(System.out);
        consoleOutput.setFormatting(PATTERN);
        consoleOutput.setLevel(DEFAULT_LEVEL);
        logger.addOutput(consoleOutput);

        //add file output
        try {
            FileOutput fileOutput = new FileOutput(LOG_OUTPUT);
            fileOutput.setFormatting(PATTERN);
            fileOutput.setLevel(DEFAULT_LEVEL);
            logger.addOutput(fileOutput);
        } catch (Exception e) {
            logger.error("Failed to create log file", e);
        }

        //debug output
        logger.addOutput(DebugScreen.LOG_OUTPUT);
    }

    private static Exception saveOldLog() {
        //if a log file already exists, check if it is the same date as today, if not, gzip it then delete the old file
        if (!Files.exists(LOG_OUTPUT))
            return null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        String fileDate;

        try {
            fileDate = sdf.format(new Date(Files.getLastModifiedTime(LOG_OUTPUT).toMillis()));
            if (today.equals(fileDate))
                return null;
        } catch (Exception e) {
            return e;
        }

        try {
            String fileName = fileDate + ".log.gz";
            Path writePath = IOUtils.parseNonDuplicatePath(LOG_OUTPUT.resolveSibling(fileName));
            IOUtils.writeFileCompressed(writePath, IOUtils.readFile(LOG_OUTPUT));
            Files.delete(LOG_OUTPUT);
        } catch (Exception e) {
            return e;
        }

        return null;
    }

    public static void debugLogLevels(Logger logger) {
        logger.trace("trace!");
        logger.debug("debug!");
        logger.info("info!");
        logger.warn("warn!");
        logger.error("error!");
        logger.fatal("fatal!");
        logger.error("exception!", new Exception("exception!"));
        System.out.println("System.out test");
        System.err.println("System.err test");
    }
}
