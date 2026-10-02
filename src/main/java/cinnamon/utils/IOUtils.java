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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class for handling I/O operations, including reading and writing files, resources, and directories<br>
 * This class provides methods for reading resources from the classpath or filesystem, writing files, and managing directories<br>
 * It also includes methods for handling compressed files, validating filenames, and opening files or URLs in the default applications
 */
public final class IOUtils {

    /**
     * The root folder for this application I/O operations, resolved from the working directory and the defined namespace
     */
    public static final Path ROOT_FOLDER = Path.of(ArgsOptions.WORKING_DIR.get()).resolve(Cinnamon.NAMESPACE);
    /**
     * A regex pattern for invalid filenames in windows, including reserved names and invalid characters
     */
    public static final String INVALID_FILENAME_REGEX = "CON|PRN|AUX|NUL|COM\\d|LPT\\d|[\\\\/:*?\"<>|\u0000]|\\.$";

    private static String resolveResourcePath(Resource res) {
        //fixes the resource path to be compatible with the classpath resource loader
        return "resources/" + res.getNamespace() + "/" + res.getPath();
    }

    /**
     * Returns an {@link InputStream} for a given resource<br>
     * If the resource has no namespace, it will be treated as a file path<br>
     * If the resource has a namespace, it will be treated as a classpath resource
     * @param res The resource to get an {@link InputStream} for
     * @return An {@link InputStream} for the given resource, or {@code null} if the resource does not exist
     */
    public static InputStream getResource(Resource res) {
        if (res.getNamespace().isEmpty()) {
            String path = res.getPath();
            try {
                return readURLStream(URI.create(path).toURL());
            } catch (IllegalArgumentException | MalformedURLException ignored) {
                return readFileStream(Path.of(path));
            }
        }

        String resourcePath = resolveResourcePath(res);
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    }

    /**
     * Reads a resource and returns its contents as a {@link ByteBuffer}<br>
     * If the resource does not exist, this method will throw a {@link RuntimeException}
     * @param res The resource to read
     * @return A {@link ByteBuffer} containing the contents of the resource
     * @throws RuntimeException If the resource does not exist or an I/O error occurs
     */
    public static ByteBuffer getResourceBuffer(Resource res) {
        InputStream stream = getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);
        return getBufferForStream(stream);
    }

    /**
     * Checks if a resource exists in the classpath or as a file on the filesystem<br>
     * If the resource has no namespace, it will be treated as a file path and checked for existence on the filesystem<br>
     * If the resource has a namespace, it will be treated as a classpath resource and checked for existence in the classpath
     * @param res The resource to check for existence
     * @return {@code true} if the resource exists, {@code false} otherwise
     */
    public static boolean hasResource(Resource res) {
        if (res.getNamespace().isEmpty()) {
            String path = res.getPath();
            try {
                return URLExists(URI.create(path).toURL());
            } catch (IllegalArgumentException | MalformedURLException ignored) {
                return Files.exists(Path.of(path));
            }
        }

        String resourcePath = resolveResourcePath(res);
        return Thread.currentThread().getContextClassLoader().getResource(resourcePath) != null;
    }

    /**
     * Reads an {@link InputStream} and returns its contents as a {@link ByteBuffer}<br>
     * This method will read the stream in chunks of 8192 bytes and will automatically resize the buffer if necessary<br>
     * The returned {@link ByteBuffer} will be flipped and ready for reading
     * @param stream The {@link InputStream} to read
     * @return A {@link ByteBuffer} containing the contents of the stream
     * @throws RuntimeException If an I/O error occurs
     */
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

    /**
     * Reads a resource and returns its contents as a string<br>
     * If the resource does not exist, this method will throw a {@link RuntimeException}
     * @param res The resource to read
     * @return A string containing the contents of the resource
     * @throws RuntimeException If the resource does not exist or an I/O error occurs
     */
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

    /**
     * Reads a GZIP compressed resource and returns its contents as a byte array<br>
     * If the resource does not exist, this method will throw a {@link RuntimeException}
     * @param res The resource to read
     * @return A byte array containing the contents of the GZIP compressed resource
     * @throws RuntimeException If the resource does not exist or an I/O error occurs
     */
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

    /**
     * Returns a list of all resources found in the specified resource folder<br>
     * This method will search for all resources in the classpath and return the unique resources found
     * @param res The resource folder to search for resources in
     * @param includeDirectories Whether to include directories in the returned list
     * @return A list of all resources found in the specified resource folder
     * @see #listNamespaces()
     */
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

    /**
     * Returns a list of all namespaces found in the resources folder<br>
     * This method will search for all resources in the classpath and return the unique namespaces found
     * @return A list of all namespaces found in the resources folder
     * @see #listResources(Resource, boolean)
     */
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

    /**
     * Returns a list of namespaces with the vanilla namespace first, followed by the other namespaces in alphabetical order
     * @return The namespaces list
     * @see #listNamespaces()
     */
    public static List<String> listNamespacesVanillaFirst() {
        List<String> namespaces = listNamespaces();
        namespaces.sort((a, b) -> {
            if (a.equals(Resource.VANILLA_NAMESPACE))
                return -1;
            if (b.equals(Resource.VANILLA_NAMESPACE))
                return 1;
            return 0;
        });
        return namespaces;
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

    /**
     * Checks if a URL exists by sending a HEAD request and checking the response code<br>
     * If the URL does not exist or an I/O error occurs, this method will return {@code false}
     * @param url The URL to check
     * @return {@code true} if the URL exists, {@code false} otherwise
     */
    public static boolean URLExists(URL url) {
        try {
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            int responseCode = huc.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads the contents of a URL and returns an {@link InputStream} to read its contents<br>
     * If the URL does not exist, this method will return {@code null}
     * @param url The URL to read
     * @return An {@link InputStream} to read the contents of the URL, or {@code null} if the URL does not exist
     * @throws RuntimeException If an I/O error occurs
     */
    public static InputStream readURLStream(URL url) {
        if (!URLExists(url))
            return null;
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a file at the specified path and returns an {@link InputStream} to read its contents<br>
     * If the file does not exist, this method will return {@code null}
     * @param path The path to the file to read
     * @return An {@link InputStream} to read the contents of the file, or {@code null} if the file does not exist
     * @throws RuntimeException If an I/O error occurs
     */
    public static InputStream readFileStream(Path path) {
        if (!Files.exists(path))
            return null;
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a file at the specified path and returns its contents as a byte array<br>
     * If the file does not exist, this method will return {@code null}
     * @param path The path to the file to read
     * @return A byte array containing the contents of the file, or {@code null} if the file does not exist
     * @throws RuntimeException If an I/O error occurs
     */
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

    /**
     * Reads a GZIP compressed file at the specified path and returns its contents as a byte array<br>
     * If the file does not exist, this method will return {@code null}
     * @param path The path to the GZIP compressed file to read
     * @return A byte array containing the contents of the GZIP compressed file, or {@code null} if the file does not exist
     * @throws RuntimeException If an I/O error occurs
     */
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

    /**
     * Writes a byte array to a file at the specified path<br>
     * The file will be created alongside any necessary parent directories
     * @param path The path to the file to write the bytes to
     * @param bytes The byte array to write to the file
     * @throws RuntimeException If an I/O error occurs
     */
    public static void writeFile(Path path, byte[] bytes) {
        try {
            //ensure path exists
            createOrGetFile(path);

            //write bytes to file
            OutputStream fs = Files.newOutputStream(path);
            fs.write(bytes);

            //close file
            fs.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes a byte array to a file at the specified path in GZIP compressed format<br>
     * The file will be created alongside any necessary parent directories
     * @param path The path to the file to write the bytes to
     * @param bytes The byte array to write to the file
     * @throws RuntimeException If an I/O error occurs
     */
    public static void writeFileCompressed(Path path, byte[] bytes) {
        try {
            //ensure path exists
            createOrGetFile(path);

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

    /**
     * Parses a path and returns a new path that does not already exist by appending a number to the filename
     * @param path The path to parse
     * @return A new path that does not already exist
     * @see #parseNonDuplicatePath(Path, String, String)
     */
    public static Path parseNonDuplicatePath(Path path) {
        return parseNonDuplicatePath(path, "_", "");
    }

    /**
     * Parses a path and returns a new path that does not already exist by appending a number to the filename<br>
     * For example, if the path {@code /path/to/file.txt} already exists, this method will return {@code /path/to/file_1.txt}, and if that also exists, it will return {@code /path/to/file_2.txt}, and so on<br>
     * @param path The path to parse
     * @param prefix The prefix to append to the filename before the number
     * @param suffix The suffix to append to the filename after the number
     * @return A new path that does not already exist
     * @see #parseNonDuplicatePath(Path)
     */
    public static Path parseNonDuplicatePath(Path path, String prefix, String suffix) {
        //return path as is if it already does not exist
        if (!Files.exists(path))
            return path;

        //grab file name and extension
        String fileName = path.getFileName().toString();
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            extension = fileName.substring(dotIndex);
            fileName = fileName.substring(0, dotIndex);
        }

        //iterate until a unique path is found
        int i = 1;
        while (Files.exists(path))
            path = path.resolveSibling(fileName + prefix + i++ + extension + suffix);

        //return new unique path
        return path;
    }

    /**
     * Ensures that the parent directory of a given path exists, creating it if necessary<br>
     * If the parent directory already exists, this method will do nothing
     * @param path The path to ensure the parent directory exists for
     * @throws RuntimeException If an I/O error occurs
     */
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

    /**
     * Creates a file at the specified path if it does not already exist<br>
     * If the file already exists, this method will do nothing<br>
     * This method will also ensure that the parent directory exists, creating it if necessary
     * @param path The path to the file to create
     * @throws RuntimeException If an I/O error occurs
     */
    public static void createOrGetFile(Path path) {
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

    /**
     * Creates a directory at the specified path if it does not already exist<br>
     * If the directory already exists, this method will do nothing
     * @param path The path to the directory to create
     * @throws RuntimeException If an I/O error occurs
     */
    public static void createOrGetDir(Path path) {
        try {
            if (!Files.exists(path))
                Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the folder of a given path, or its parent if the path is a file
     * @param path The path to get the folder or parent of
     * @return The folder of the given path, or its parent if the path is a file
     */
    public static Path getFolderOrParent(Path path) {
        if (Files.isDirectory(path))
            return path;
        return path.getParent();
    }

    /**
     * Opens a file in the default application for its file type
     * @param path The path to the file to open
     * @throws RuntimeException If an I/O error occurs
     */
    public static void openFile(Path path) {
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens a file or folder in the system file explorer<br>
     * If the path is a file, the parent folder will be opened instead
     * @param path The path to the file or folder to open in the system file explorer
     * @throws RuntimeException If an I/O error occurs
     */
    public static void openInExplorer(Path path) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            Desktop.getDesktop().browseFileDirectory(path.toFile());
            return;
        }

        try {
            Desktop.getDesktop().open(getFolderOrParent(path).toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens a URL in the default web browser
     * @param url The URL to open
     * @throws RuntimeException If an error occurs
     */
    public static void openURL(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes a {@link BufferedImage} to a file at the specified path in PNG format<br>
     * The file will be created alongside any necessary parent directories
     * @param path The path to the file to write the image to
     * @param image The {@link BufferedImage} to write to the file
     * @throws RuntimeException If an I/O error occurs
     */
    public static void writeImage(Path path, BufferedImage image) {
        try {
            //ensure path exists
            createOrGetFile(path);

            //write image to output stream
            OutputStream fs = Files.newOutputStream(path);
            ImageIO.write(image, "png", fs);

            //close file
            fs.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a directory and all of its contents recursively<br>
     * If the directory does not exist, this method will do nothing
     * @param path The path to the directory to delete
     * @throws RuntimeException If an I/O error occurs
     */
    public static void deleteDir(Path path) {
        try {
            if (!Files.exists(path))
                return;

            //delete subdirectories and files
            try (Stream<Path> stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if a filename is valid according to the {@link #INVALID_FILENAME_REGEX} pattern<br>
     * This will fail for {@code paths} as they contain the directory separator {@code /} which is not allowed in filenames
     * @param filename The filename to check
     * @return {@code true} if the filename is valid, {@code false} otherwise
     */
    public static boolean isValidFilename(String filename) {
        if (filename.isBlank())
            return false;
        Pattern pattern = Pattern.compile(INVALID_FILENAME_REGEX);
        return !pattern.matcher(filename).find();
    }

    /**
     * Returns the parent directory of a given path
     * @param path The path to get the parent directory of
     * @return The parent directory of the given path, or an empty string if no parent exists
     */
    public static String getParent(String path) {
        if (path.isBlank())
            return "";

        int slash = path.lastIndexOf('/');
        return slash == -1 ? "" : path.substring(0, slash);
    }

    /**
     * Returns the filename of a given path
     * @param path The path to get the filename of
     * @return The filename of the given path alongside its extension, or an empty string for invalid filenames
     */
    public static String getFilename(String path) {
        return path.isBlank() ? "" : path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Returns the extension of a given filename
     * @param filename The filename to get the extension of
     * @return The extension of the given filename, or an empty string if no extension was found
     */
    public static String getExtension(String filename) {
        if (filename.isBlank())
            return "";

        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot + 1);
    }

    /**
     * Returns the filename without its extension from a given path
     * @param path The path to get the filename without extension of
     * @return The filename without its extension from the given path, or an empty string for invalid paths
     */
    public static String getFilenameWithoutExtension(String path) {
        if (path.isBlank())
            return "";

        String file = getFilename(path);
        int dot = file.lastIndexOf('.');
        return dot == -1 ? file : file.substring(0, dot);
    }

    /**
     * Resolves a path against a given root path<br>
     * If the root path is blank, the path will be returned as is<br>
     * If the path is blank, the root path will be returned as is<br>
     * Otherwise, the path will be appended to the root path with a {@code /} separator
     * @param rootPath The root path to resolve the path against
     * @param path The path to resolve against the rootPath
     * @return The resolved root path with the path appended, or the root path or path as is if one of them is blank
     */
    public static String resolve(String rootPath, String path) {
        if (rootPath.isBlank())
            return path;
        if (path.isBlank())
            return rootPath;

        return rootPath.endsWith("/") ? rootPath + path : rootPath + "/" + path;
    }

    /**
     * Resolves a path against the parent directory of a given root path<br>
     * If the root path is blank, the path will be returned as is<br>
     * If the path is blank, the root path will be returned as is<br>
     * Otherwise, the path will be appended to the parent directory of the root path with a {@code /} separator
     * @param rootPath The root path to resolve the path against its parent directory
     * @param path The path to resolve against the parent directory of the root path
     * @return The resolved root path with the path appended to the parent directory, or the root path or path as is if one of them is blank
     */
    public static String resolveSibling(String rootPath, String path) {
        if (rootPath.isBlank())
            return path;
        if (path.isBlank())
            return rootPath;

        int slash = rootPath.lastIndexOf('/');
        return slash == -1 ? path : rootPath.substring(0, slash + 1) + path;
    }

    /**
     * A comparator for filenames that compares them in a natural order, taking into account both string and numeric parts of the filenames<br>
     * For example, {@code file1} will be considered less than {@code file2}, which will be considered less than {@code file10}
     */
    public static final class FilenameComparator implements Comparator<String> {

        private static final FilenameComparator INSTANCE = new FilenameComparator();

        private FilenameComparator() {}

        public static int compareTo(String o1, String o2) {
            return INSTANCE.compare(o1, o2);
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
