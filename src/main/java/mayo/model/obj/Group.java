package mayo.model.obj;

import mayo.utils.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String name;
    private final List<Face> faces = new ArrayList<>();

    private Material material;
    private boolean smooth;

    //bounding box
    private final Vector3f
            bbMin = new Vector3f(Integer.MAX_VALUE),
            bbMax = new Vector3f(Integer.MIN_VALUE);

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

    public Vector3f getBBMin() {
        return bbMin;
    }

    public Vector3f getBBMax() {
        return bbMax;
    }

    public AABB getAABB() {
        return new AABB(bbMin, bbMax);
    }
}
