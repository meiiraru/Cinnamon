package mayo.utils;

import java.util.Objects;

public class Resource {

    public static final String VANILLA_NAMESPACE = "vanilla";

    private final String namespace, path;

    public Resource(String path) {
        int index = path.indexOf(":");
        if (index > 0) {
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
        return new Resource(this.namespace, this.path + "/" + path);
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
