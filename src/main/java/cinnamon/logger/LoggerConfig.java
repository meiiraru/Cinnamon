package cinnamon.logger;

import cinnamon.utils.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.io.IoBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerConfig {

    public static final Path LOG_OUTPUT = IOUtils.ROOT_FOLDER.resolve("logs/log.log");
    private static final String PATTERN = "[%d{HH:mm:ss}] [%t/%level] (%logger{36}) %msg%n";
    private static final Level DEFAULT_LEVEL = Level.INFO;

    public static void initialize(Logger logger) {
        //save old log file
        Exception gzipException = saveOldLog();

        //configure logger
        configureLogger();

        //system out detection
        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).setAutoFlush(true).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).setAutoFlush(true).buildPrintStream());

        //debugLogLevels(logger);

        //log any gzip exceptions
        if (gzipException != null)
            logger.error("Failed to parse previous log file", gzipException);
    }

    private static void configureLogger() {
        //configure log4j
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        //console appender
        builder.add(builder
                .newAppender("Console", "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                .add(builder.newLayout("PatternLayout").addAttribute("pattern", PATTERN))
        );

        //file appender
        builder.add(builder
                .newAppender("File", "FILE")
                .addAttribute("fileName", LOG_OUTPUT.toString())
                .add(builder.newLayout("PatternLayout").addAttribute("pattern", PATTERN))
        );

        //root logger
        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(DEFAULT_LEVEL);
        rootLogger.add(builder.newAppenderRef("Console"));
        rootLogger.add(builder.newAppenderRef("File"));
        builder.add(rootLogger);

        //build new configuration
        Configurator.reconfigure(builder.build());
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
    }
}
