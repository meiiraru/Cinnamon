package cinnamon.utils;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.lwjgl.system.MemoryUtil.memSlice;

public class IOUtils {

    public static final String VANILLA_FOLDER = "cinnamon";
    public static final Path ROOT_FOLDER = Path.of("./" + VANILLA_FOLDER);

    public static InputStream getResource(Resource res) {
        if (res.getNamespace().isEmpty())
            return readFileStream(Path.of(res.getPath()));

        String resourcePath = "resources/" + res.getNamespace() + "/" + res.getPath();
        return IOUtils.class.getClassLoader().getResourceAsStream(resourcePath);
    }

    public static ByteBuffer getResourceBuffer(Resource res) {
        InputStream stream = getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);
        return getBufferForStream(stream);
    }

    public static boolean hasResource(Resource res) {
        return getResource(res) != null;
    }

    public static ByteBuffer getBufferForStream(InputStream stream) {
        try {
            ByteBuffer fontBuffer = BufferUtils.createByteBuffer(stream.available() + 1);
            Channels.newChannel(stream).read(fontBuffer);

            fontBuffer.flip();
            return memSlice(fontBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readString(Resource res) {
        InputStream stream = getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readCompressed(Resource res) {
        InputStream stream = getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try {
            try (GZIPInputStream gzip = new GZIPInputStream(stream)) {
                return gzip.readAllBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream readFileStream(Path path) {
        if (!Files.exists(path))
            return null;
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readFile(Path path) {
        if (!Files.exists(path))
            return null;

        try (InputStream stream = Files.newInputStream(path)) {
            //read bytes from file
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readFileCompressed(Path path) {
        if (!Files.exists(path))
            return null;

        try (InputStream stream = Files.newInputStream(path); GZIPInputStream gzip = new GZIPInputStream(stream)) {
            //read bytes from file
            return gzip.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeFile(Path path, byte[] bytes) {
        try {
            //ensure path exists
            createOrGetPath(path);

            //write bytes to file
            OutputStream fs = Files.newOutputStream(path);
            fs.write(bytes);

            //close file
            fs.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeFileCompressed(Path path, byte[] bytes) {
        try {
            //ensure path exists
            createOrGetPath(path);

            //write bytes to file
            OutputStream fs = Files.newOutputStream(path);
            GZIPOutputStream gzip = new GZIPOutputStream(fs);
            gzip.write(bytes);

            //close streams
            gzip.close();
            fs.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path parseNonDuplicatePath(Path path) {
        //return path as is if it already does not exist
        if (!Files.exists(path))
            return path;

        //grab file name and extension
        String fileName = path.getFileName().toString();
        String extension = "";
        int dotIndex = fileName.indexOf('.');
        if (dotIndex != -1) {
            extension = fileName.substring(dotIndex);
            fileName = fileName.substring(0, dotIndex);
        }

        //iterate until a unique path is found
        int i = 1;
        while (Files.exists(path))
            path = path.resolveSibling(fileName + "_" + i++ + extension);

        //return new unique path
        return path;
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

    public static void createOrGetPath(Path path) {
        try {
            //ensure dir exists
            ensureParentExists(path);

            //create file if non-existent
            if (!Files.exists(path))
                Files.createFile(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void openFile(Path path) {
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void openInExplorer(Path path) {
        try {
            Desktop.getDesktop().open(path.toFile().isDirectory() ? path.toFile() : path.getParent().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeImage(Path path, BufferedImage image) {
        try {
            //ensure path exists
            createOrGetPath(path);

            //write image to output stream
            OutputStream fs = Files.newOutputStream(path);
            ImageIO.write(image, "png", fs);

            //close file
            fs.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
