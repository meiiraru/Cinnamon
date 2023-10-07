package mayo.model.obj;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String name;
    private final List<Face> faces = new ArrayList<>();

    private Material material;
    private boolean smooth;

    public Group(String name) {
        this.name = name;
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }

    public String getName() {
        return name;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }
}
