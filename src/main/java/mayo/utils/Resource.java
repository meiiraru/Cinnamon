package mayo.utils;

public class Resource {

    private static final String NAMESPACE = "mayo";

    private final String namespace, path;

    public Resource(String path) {
        this(NAMESPACE, path);
    }

    public Resource(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return namespace + "/" + path;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Resource r && r.namespace.equals(namespace) && r.path.equals(path);
    }
}
