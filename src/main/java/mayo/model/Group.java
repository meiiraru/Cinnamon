package mayo.model;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String name;
    private final List<Face> faces = new ArrayList<>();

    private String material;
    private boolean smooth;

    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }
}
