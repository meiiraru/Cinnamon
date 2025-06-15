package cinnamon.logger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

public class LoggerStream extends PrintStream {

    public LoggerStream(Consumer<String> stringConsumer) {
        super(new LoggerOutputStream(stringConsumer));
    }

    private static class LoggerOutputStream extends OutputStream {
        private final StringBuilder buffer = new StringBuilder();
        private final Consumer<String> stringConsumer;

        private LoggerOutputStream(Consumer<String> stringConsumer) {
            this.stringConsumer = stringConsumer;
        }

        @Override
        public void write(int b) {
            char c = (char) b;
            if (c != '\n') {
                buffer.append(c);
            } else if (!buffer.isEmpty()) {
                stringConsumer.accept(buffer.toString());
                buffer.setLength(0);
            }
        }
    }
}
