package cinnamon.logger;

public enum Level {
    TRACE("\u001B[90m"),
    DEBUG("\u001B[90m"),
    INFO("\u001B[97m"),
    WARN("\u001B[93m"),
    ERROR("\u001B[91m"),
    FATAL("\u001B[95m");

    public static final String ANSI_RESET = "\u001B[0m";

    public final String ansiCode;

    Level(String ansiCode) {
        this.ansiCode = ansiCode;
    }
}
