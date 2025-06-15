package cinnamon.logger;

import java.io.PrintStream;

public class ConsoleOutput extends LogOutput {

    private final PrintStream out;

    public ConsoleOutput(PrintStream printStream) {
        this.out = printStream;
    }

    @Override
    public void write(Level level, String message, Throwable throwable) {
        out.print(message);
        if (throwable != null)
            throwable.printStackTrace(out);
    }
}
