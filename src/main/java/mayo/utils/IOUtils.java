package mayo.utils;

import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.system.MemoryUtil.memSlice;

public class IOUtils {

    public static InputStream getResource(String namespace, String path) {
        String resourcePath = "resources/" + namespace + "/" + path;
        InputStream resource = IOUtils.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resource == null)
            throw new RuntimeException("Resource not found: " + resourcePath);
        return resource;
    }

    public static ByteBuffer getResourceBuffer(String namespace, String path) {
        try {
            InputStream stream = getResource(namespace, path);
            ByteBuffer fontBuffer = BufferUtils.createByteBuffer(stream.available() + 1);
            Channels.newChannel(stream).read(fontBuffer);

            fontBuffer.flip();
            return memSlice(fontBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readFileBytes(Path file) {
        if (!Files.exists(file))
            return null;

        try (InputStream stream = Files.newInputStream(file)) {
            //read bytes from file
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeFile(Path file, byte[] bytes) {
        try {
            //ensure dir exists
            ensureParentExists(file);

            //create file if non-existent
            if (!Files.exists(file))
                Files.createFile(file);

            //write bytes to file
            OutputStream fs = Files.newOutputStream(file);
            fs.write(bytes);

            //close file
            fs.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void ensureParentExists(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null)
                Files.createDirectories(parent);
        } catch (FileAlreadyExistsException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
