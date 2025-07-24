package cinnamon.utils;

import java.util.Objects;

public class Resource {

    public static final String VANILLA_NAMESPACE = "vanilla";

    private final String namespace, path;

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

    public Resource(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public Resource resolve(String path) {
        return new Resource(this.namespace, this.path.endsWith("/") ? this.path + path : this.path + "/" + path);
    }

    public Resource resolveSibling(String path) {
        return new Resource(this.namespace, this.path.substring(0, this.path.lastIndexOf("/") + 1) + path);
    }

    public String getFileName() {
        int index = path.lastIndexOf("/");
        return index > -1 ? path.substring(index + 1) : path;
    }

    public String getFileNameWithoutExtension() {
        String fileName = getFileName();
        int index = fileName.lastIndexOf(".");
        return index > -1 ? fileName.substring(0, index) : fileName;
    }

    public String getExtension() {
        int index = path.lastIndexOf(".");
        return index > -1 ? path.substring(index + 1) : "";
    }

    public String getNamespace() {
        return namespace;
    }

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
