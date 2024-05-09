package mayo.logger;

import mayo.utils.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerConfig {

    public static final Path LOG_OUTPUT = IOUtils.ROOT_FOLDER.resolve("logs/log.log");
    private static final String PATTERN = "[%d{HH:mm:ss}] [%t/%level] (%logger{36}) %msg%n";
    private static final Level DEFAULT_LEVEL = Level.INFO;

    public static void initialize(Logger logger) {
        //if a log file already exists, gzip it then delete the old file
        Exception gzipException = null;
        if (Files.exists(LOG_OUTPUT)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String fileName = sdf.format(new Date()) + ".log.gz";
                Path writePath = IOUtils.parseNonDuplicatePath(LOG_OUTPUT.resolveSibling(fileName));
                IOUtils.writeFile(writePath, IOUtils.readFile(LOG_OUTPUT));
                Files.delete(LOG_OUTPUT);
            } catch (Exception e) {
                gzipException = e;
            }
        }

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

        //log any gzip exceptions
        if (gzipException != null)
            logger.error("Failed to gzip previous log file", gzipException);
    }
}
