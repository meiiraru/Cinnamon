package cinnamon.logger;

import cinnamon.utils.IOUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public class FileOutput extends LogOutput {

    private final PrintWriter fileWriter;

    public FileOutput(Path path) throws IOException {
        IOUtils.createOrGetPath(path);
        this.fileWriter = new PrintWriter(new FileWriter(path.toFile(), true), false);
    }

    @Override
    public void write(String message, Throwable throwable) {
        fileWriter.print(message);
        if (throwable != null)
            throwable.printStackTrace(fileWriter);
        fileWriter.flush();
    }
}
