package mayo.utils;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.lwjgl.system.MemoryUtil.memSlice;

public class IOUtils {

    public static final String VANILLA_FOLDER = "mayonnaise";
    public static final Path ROOT_FOLDER = Path.of("./" + VANILLA_FOLDER);

    public static InputStream getResource(Resource res) {
        String resourcePath = "resources/" + res.getNamespace() + "/" + res.getPath();
        return IOUtils.class.getClassLoader().getResourceAsStream(resourcePath);
    }

    public static ByteBuffer getResourceBuffer(Resource res) {
        try {
            InputStream stream = getResource(res);

            if (stream == null)
                throw new RuntimeException("Resource not found: " + res);

            ByteBuffer fontBuffer = BufferUtils.createByteBuffer(stream.available() + 1);
            Channels.newChannel(stream).read(fontBuffer);

            fontBuffer.flip();
            return memSlice(fontBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasResource(Resource res) {
        return getResource(res) != null;
    }

    public static String readString(Resource res) {
        try {
            InputStream stream = getResource(res);
            if (stream == null)
                throw new RuntimeException("Resource not found: " + res);

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void readStringLines(Resource res, BiConsumer<String, Integer> lineConsumer) {
        try {
            InputStream stream = getResource(res);
            if (stream == null)
                throw new RuntimeException("Resource not found: " + res);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
                int i = 1;
                String line;
                while ((line = br.readLine()) != null)
                    lineConsumer.accept(line, i++);
            }
        } catch (Exception e) {
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

    public static void openFileInExplorer(Path path) {
        try {
            Desktop.getDesktop().open(path.toFile().isDirectory() ? path.getParent().toFile() : path.toFile());
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
