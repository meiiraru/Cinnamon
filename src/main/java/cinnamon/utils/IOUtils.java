package cinnamon.utils;

import cinnamon.Cinnamon;
import cinnamon.settings.ArgsOptions;
import org.joml.Math;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IOUtils {

    public static final Path ROOT_FOLDER = Path.of(ArgsOptions.WORKING_DIR.getAsString()).resolve(Cinnamon.NAMESPACE);

    private static String resolveResourcePath(Resource res) {
        return "resources/" + res.getNamespace() + "/" + res.getPath();
    }

    public static InputStream getResource(Resource res) {
        if (res.getNamespace().isEmpty())
            return readFileStream(Path.of(res.getPath()));

        String resourcePath = resolveResourcePath(res);
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    }

    public static ByteBuffer getResourceBuffer(Resource res) {
        InputStream stream = getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);
        return getBufferForStream(stream);
    }

    public static boolean hasResource(Resource res) {
        if (res.getNamespace().isEmpty())
            return Files.exists(Path.of(res.getPath()));

        String resourcePath = resolveResourcePath(res);
        return Thread.currentThread().getContextClassLoader().getResource(resourcePath) != null;
    }

    public static ByteBuffer getBufferForStream(InputStream stream) {
        try (stream) {
            ByteBuffer buffer = BufferUtils.createByteBuffer(8192);
            byte[] chunk = new byte[8192];

            int bytesRead;
            while ((bytesRead = stream.read(chunk)) != -1) {
                if (buffer.remaining() < bytesRead) {
                    int newCapacity = Math.max(buffer.capacity() * 2, buffer.capacity() - buffer.remaining() + bytesRead);
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
                buffer.put(chunk, 0, bytesRead);
            }

            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readString(Resource res) {
        InputStream stream = getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readCompressed(Resource res) {
        InputStream stream = getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; GZIPInputStream gzip = new GZIPInputStream(stream)) {
            return gzip.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> listResources(Resource res, boolean includeDirectories) {
        try {
            String path = resolveResourcePath(res);
            if (res.getNamespace().isEmpty())
                return listResources(Path.of(res.getPath()).toUri().toURL(), path, includeDirectories);

            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(path);
            List<String> result = new ArrayList<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                List<String> entries = listResources(url, path, includeDirectories);
                if (entries != null)
                    result.addAll(entries);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> listNamespaces() {
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources("resources");
            List<String> result = new ArrayList<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                List<String> entries = listResources(url, "resources", true);
                if (entries != null)
                    result.addAll(entries);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //https://stackoverflow.com/a/49570879
    private static List<String> listResources(URL url, String pathStr, boolean includeDirectories) {
        if (url == null)
            return null;

        try {
            URI uri = url.toURI();
            if (uri.getScheme().equals("jar")) { //jar packed resource
                try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    Path path = fileSystem.getPath(pathStr);

                    //get all contents of a resource (skip resource itself)
                    try (Stream<Path> stream = Files.walk(path, 1).skip(1)) {
                        return stream
                                .filter(p -> includeDirectories || !Files.isDirectory(p))
                                .map(p -> p.getFileName().toString())
                                .collect(Collectors.toList());
                    }
                }
            } else { //file system resource
                File resource = new File(uri);
                String[] files = resource.list();
                return files == null ? null : includeDirectories ? Arrays.asList(files) : Arrays.stream(files).filter(f -> !new File(resource, f).isDirectory()).collect(Collectors.toList());
            }
        } catch (Exception e) {
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

    public static class FilenameComparator implements Comparator<String> {

        public static int compareTo(String o1, String o2) {
            return new FilenameComparator().compare(o1, o2);
        }

        @Override
        public int compare(String o1, String o2) {
            //compare file names based on either string or number values
            //file1 < file2 < file10

            //split file names into parts
            String[] parts1 = o1.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            String[] parts2 = o2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

            //iterate over parts
            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                //compare parts
                int result;
                if (Character.isDigit(parts1[i].charAt(0)) && Character.isDigit(parts2[i].charAt(0))) {
                    //compare as numbers
                    result = Integer.compare(Integer.parseInt(parts1[i]), Integer.parseInt(parts2[i]));
                } else {
                    //compare as strings
                    result = parts1[i].compareTo(parts2[i]);
                }

                //return result if parts are not equal
                if (result != 0)
                    return result;
            }

            //return comparison based on part count
            return Integer.compare(parts1.length, parts2.length);
        }
    }
}
