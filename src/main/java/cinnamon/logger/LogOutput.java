package cinnamon.logger;

import java.util.Calendar;

public abstract class LogOutput {

    private Level level = Level.DEBUG;
    private String formatting = "[%3$s] (%4$s) %5$s\n";

    public abstract void write(Level level, String message, Throwable throwable);

    public boolean shouldLog(Level level) {
        return this.level.compareTo(level) <= 0;
    }

    public String applyFormatting(Calendar calendar, String threadName, Level level, String className, String message) {
        return String.format(formatting, calendar, threadName, level.name(), className, message);
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setFormatting(String formatting) {
        this.formatting = formatting;
    }
}
