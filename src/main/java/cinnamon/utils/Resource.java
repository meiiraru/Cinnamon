package cinnamon.utils;

import java.util.Objects;

/**
 * Represents a resource with a namespace and a path
 */
public class Resource {

    /**
     * The default namespace for resources without a specified namespace
     */
    public static final String VANILLA_NAMESPACE = "vanilla";

    private final String namespace, path;

    /**
     * Creates a new resource from a string representation of the resource, separated by {@code :}<br>
     * If no namespace is specified, the default namespace {@link #VANILLA_NAMESPACE} will be used
     * @param path The string representation of the resource
     */
    public Resource(String path) {
        int index = path.indexOf(":");
        if (index > -1) {
            this.namespace = path.substring(0, index);
            this.path = path.substring(index + 1);
        } else {
            this.namespace = VANILLA_NAMESPACE;
            this.path = path;
        }
    }

    /**
     * Creates a new resource with the specified namespace and path
     * @param namespace The namespace of the resource
     * @param path The path of the resource
     */
    public Resource(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    /**
     * Resolves a new resource relative to this resource path
     * @param path The path to resolve
     * @return A new resource with the same namespace and the resolved path
     */
    public Resource resolve(String path) {
        return new Resource(getNamespace(), IOUtils.resolve(getPath(), path));
    }

    /**
     * Resolves a new resource relative to this resource path, but in the same directory as this resource
     * @param path The path to resolve
     * @return A new resource with the same namespace and the resolved path
     */
    public Resource resolveSibling(String path) {
        return new Resource(getNamespace(),  IOUtils.resolveSibling(getPath(), path));
    }

    /**
     * Gets the parent resource of this resource
     * @return A new resource with the same namespace and the parent path
     */
    public Resource getParent() {
        return new Resource(getNamespace(), IOUtils.getParent(getPath()));
    }

    /**
     * Gets the file name of this resource
     * @return The file name of this resource
     */
    public String getFileName() {
        return IOUtils.getFilename(getPath());
    }

    /**
     * Gets the file name of this resource without the extension
     * @return The file name of this resource without the extension
     */
    public String getFileNameWithoutExtension() {
        return IOUtils.getFilenameWithoutExtension(getPath());
    }

    /**
     * Gets the extension of this resource
     * @return The extension of this resource
     */
    public String getExtension() {
        return IOUtils.getExtension(getPath());
    }

    /**
     * Gets the namespace of this resource
     * @return The namespace of this resource
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the path of this resource
     * @return The path of this resource
     */
    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Resource r && r.namespace.equals(namespace) && r.path.equals(path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
